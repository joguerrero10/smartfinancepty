package com.smartfinancepty.finance.service.analytics.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.Expense;
import com.smartfinancepty.finance.domain.ExpenseCategory;
import com.smartfinancepty.finance.domain.Income;
import com.smartfinancepty.finance.graphql.dto.*;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;

    // ── Analytics del mes ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> expenses = expenseRepository
                .findByUserIdAndExpenseDateBetweenAndActiveTrue(userId, start, end);

        List<Income> incomes = incomeRepository.findByUserIdAndActiveTrueWithDeductions(userId);

        BigDecimal totalExpenses =
                expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = calcNetIncome(incomes);

        double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? totalIncome.subtract(totalExpenses).divide(totalIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // Top categorías del mes actual vs mes anterior
        List<CategoryAnalytic> topCategories =
                buildCategoryAnalytics(userId, expenses, totalExpenses, year, month);

        // Tendencia 6 meses
        List<TrendPoint> trend = buildTrend(userId, year, month, 6);

        // Predicción próximo mes
        PredictionResult prediction = buildPrediction(userId, year, month, totalIncome);

        // Nivel de riesgo
        String[] risk = calcRiskLevel(savingsRate, totalExpenses, totalIncome);

        return AnalyticsResponse.builder().year(year).month(month).totalExpenses(totalExpenses)
                .totalIncome(totalIncome).savingsRate(Math.round(savingsRate * 100.0) / 100.0)
                .topCategories(topCategories).expenseTrend(trend).prediction(prediction)
                .riskLevel(risk[0]).riskMessage(risk[1]).build();
    }

    // ── Comparación mes actual vs anterior ───────────────────────────────────

    @Transactional(readOnly = true)
    public ComparisonResponse getComparison(Long userId, int year, int month) {
        YearMonth current = YearMonth.of(year, month);
        YearMonth previous = current.minusMonths(1);

        MonthSummary curr = buildMonthSummary(userId, current);
        MonthSummary prev = buildMonthSummary(userId, previous);

        BigDecimal diff = curr.getTotalExpenses().subtract(prev.getTotalExpenses());
        boolean increased = diff.compareTo(BigDecimal.ZERO) > 0;

        double changePct = prev.getTotalExpenses().compareTo(BigDecimal.ZERO) > 0
                ? diff.divide(prev.getTotalExpenses(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // Cambios por categoría
        Map<String, BigDecimal> currByCat = expensesByCategoryMap(userId, current);
        Map<String, BigDecimal> prevByCat = expensesByCategoryMap(userId, previous);

        Set<String> allCats = new HashSet<>();
        allCats.addAll(currByCat.keySet());
        allCats.addAll(prevByCat.keySet());

        List<CategoryChange> increased_cats = new ArrayList<>();
        List<CategoryChange> decreased_cats = new ArrayList<>();

        for (String cat : allCats) {
            BigDecimal currAmt = currByCat.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal prevAmt = prevByCat.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal change = currAmt.subtract(prevAmt);

            double pct = prevAmt.compareTo(BigDecimal.ZERO) > 0
                    ? change.divide(prevAmt, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : (currAmt.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);

            CategoryChange cc = CategoryChange.builder().categoryName(cat).previousAmount(prevAmt)
                    .currentAmount(currAmt).changeAmount(change)
                    .changePercentage(Math.round(pct * 100.0) / 100.0).build();

            if (change.compareTo(BigDecimal.ZERO) > 0)
                increased_cats.add(cc);
            else if (change.compareTo(BigDecimal.ZERO) < 0)
                decreased_cats.add(cc);
        }

        increased_cats.sort(Comparator.comparing(CategoryChange::getChangePercentage).reversed());
        decreased_cats.sort(Comparator.comparing(CategoryChange::getChangePercentage));

        return ComparisonResponse.builder().currentMonth(curr).previousMonth(prev)
                .expenseDifference(diff)
                .expenseChangePercentage(Math.round(changePct * 100.0) / 100.0).increased(increased)
                .categoriesIncreased(increased_cats).categoriesDecreased(decreased_cats).build();
    }

    // ── Recomendaciones IA ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendations(Long userId) {
        List<Recommendation> recs = new ArrayList<>();
        LocalDate now = LocalDate.now();
        int year = now.getYear(), month = now.getMonthValue();

        YearMonth ym = YearMonth.of(year, month);
        List<Expense> currExpenses =
                expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(userId,
                        ym.atDay(1), ym.atEndOfMonth());

        List<Income> incomes = incomeRepository.findByUserIdAndActiveTrueWithDeductions(userId);
        BigDecimal netIncome = calcNetIncome(incomes);

        BigDecimal totalExpenses = currExpenses.stream().map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double savingsRate = netIncome.compareTo(BigDecimal.ZERO) > 0
                ? netIncome.subtract(totalExpenses).divide(netIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // 1. Riesgo de sobregasto
        if (savingsRate < 0) {
            recs.add(Recommendation.builder().id("RISK_001").type("RISK")
                    .title("⚠️ Estás gastando más de lo que ganas")
                    .message("Tus gastos este mes superan tus ingresos netos en $"
                            + totalExpenses.subtract(netIncome).setScale(2, RoundingMode.HALF_UP)
                            + ". Revisa tus gastos variables urgentemente.")
                    .priority("HIGH").actionable(true).build());
        } else if (savingsRate < 10) {
            recs.add(Recommendation.builder().id("RISK_002").type("RISK")
                    .title("🔔 Tu tasa de ahorro es muy baja")
                    .message("Solo estás ahorrando el " + String.format("%.1f", savingsRate)
                            + "% de tus ingresos. Se recomienda al menos un 20%.")
                    .priority("HIGH").actionable(true).build());
        }

        // 2. Patrones de gasto por categoría vs mes anterior
        Map<String, BigDecimal> currByCat = expensesByCategoryMap(userId, ym);
        Map<String, BigDecimal> prevByCat = expensesByCategoryMap(userId, ym.minusMonths(1));

        for (Map.Entry<String, BigDecimal> entry : currByCat.entrySet()) {
            String cat = entry.getKey();
            BigDecimal curr = entry.getValue();
            BigDecimal prev = prevByCat.getOrDefault(cat, BigDecimal.ZERO);

            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                double pct = curr.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();

                if (pct >= 30) {
                    recs.add(Recommendation.builder()
                            .id("PATTERN_" + cat.toUpperCase().replace(" ", "_"))
                            .type("SPENDING_PATTERN")
                            .title("📈 Gastos en " + cat + " aumentaron "
                                    + String.format("%.0f", pct) + "%")
                            .message("Tus gastos en " + cat + " aumentaron "
                                    + String.format("%.1f", pct) + "% vs el mes pasado. "
                                    + "El mes pasado gastaste $"
                                    + prev.setScale(2, RoundingMode.HALF_UP)
                                    + " y este mes llevas $"
                                    + curr.setScale(2, RoundingMode.HALF_UP) + ".")
                            .priority(pct >= 50 ? "HIGH" : "MEDIUM").categoryName(cat)
                            .actionable(true).build());
                }
            }

            // 3. Sugerencias de ahorro — categorías con alto gasto
            if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
                double catPct = curr.divide(netIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();

                if (catPct > 25) {
                    BigDecimal suggested = netIncome.multiply(BigDecimal.valueOf(0.20)).setScale(2,
                            RoundingMode.HALF_UP);
                    BigDecimal saving = curr.subtract(suggested).max(BigDecimal.ZERO);

                    recs.add(Recommendation.builder()
                            .id("SAVINGS_" + cat.toUpperCase().replace(" ", "_")).type("SAVINGS")
                            .title("💡 Reduce gastos en " + cat)
                            .message("Estás destinando el " + String.format("%.1f", catPct)
                                    + "% de tus ingresos a " + cat
                                    + ". Si lo reduces al 20%, podrías ahorrar $" + saving
                                    + " este mes.")
                            .priority("MEDIUM").categoryName(cat).potentialSavings(saving)
                            .actionable(true).build());
                }
            }
        }

        // 4. Recomendación positiva si ahorro >= 20%
        if (savingsRate >= 20) {
            recs.add(Recommendation.builder().id("POSITIVE_001").type("SAVINGS")
                    .title("✅ ¡Excelente gestión financiera!")
                    .message("Estás ahorrando el " + String.format("%.1f", savingsRate)
                            + "% de tus ingresos. Considera invertir el excedente.")
                    .priority("LOW").actionable(false).build());
        }

        // Ordenar por prioridad
        recs.sort(Comparator.comparing(r -> {
            return switch (r.getPriority()) {
                case "HIGH" -> 0;
                case "MEDIUM" -> 1;
                default -> 2;
            };
        }));

        return recs;
    }

    // ── REST DTO (para endpoint REST) ────────────────────────────────────────

    @Transactional(readOnly = true)
    public com.smartfinancepty.finance.dto.analytic.AnalyticsRestResponse getAnalyticsRest(
            Long userId, int year, int month) {
        AnalyticsResponse gql = getAnalytics(userId, year, month);
        List<Recommendation> recs = getRecommendations(userId);
        ComparisonResponse comparison = getComparison(userId, year, month);

        return com.smartfinancepty.finance.dto.analytic.AnalyticsRestResponse.builder()
                .analytics(gql).recommendations(recs).comparison(comparison).build();
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private BigDecimal calcNetIncome(List<Income> incomes) {
        return incomes.stream().map(income -> {
            BigDecimal ded = income.getDeductions().stream()
                    .map(d -> d.isPercentage()
                            ? income.getAmount().multiply(d.getValue())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : d.getValue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return income.getAmount().subtract(ded);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryAnalytic> buildCategoryAnalytics(Long userId, List<Expense> expenses,
            BigDecimal total, int year, int month) {

        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        Map<String, BigDecimal> prevByCat = expensesByCategoryMap(userId, prev);

        Map<ExpenseCategory, List<Expense>> byCat =
                expenses.stream().collect(Collectors.groupingBy(Expense::getCategory));

        return byCat.entrySet().stream().map(entry -> {
            ExpenseCategory cat = entry.getKey();
            List<Expense> catExp = entry.getValue();
            BigDecimal catTotal = catExp.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO,
                    BigDecimal::add);

            double pct = total.compareTo(BigDecimal.ZERO) > 0
                    ? catTotal.divide(total, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            BigDecimal prevAmt = prevByCat.getOrDefault(cat.getName(), BigDecimal.ZERO);
            double trendPct = prevAmt.compareTo(BigDecimal.ZERO) > 0
                    ? catTotal.subtract(prevAmt).divide(prevAmt, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            String trend = Math.abs(trendPct) < 5 ? "STABLE" : trendPct > 0 ? "UP" : "DOWN";

            return CategoryAnalytic.builder().categoryName(cat.getName()).totalAmount(catTotal)
                    .percentage(Math.round(pct * 100.0) / 100.0).trend(trend)
                    .trendPercentage(Math.round(trendPct * 100.0) / 100.0)
                    .expenseCount(catExp.size()).build();
        }).sorted(Comparator.comparing(CategoryAnalytic::getTotalAmount).reversed()).limit(5)
                .toList();
    }

    private List<TrendPoint> buildTrend(Long userId, int year, int month, int months) {
        List<TrendPoint> trend = new ArrayList<>();
        YearMonth ym = YearMonth.of(year, month);

        for (int i = months - 1; i >= 0; i--) {
            YearMonth target = ym.minusMonths(i);
            BigDecimal total = expenseRepository.sumExpensesByUserAndDateRange(userId,
                    target.atDay(1), target.atEndOfMonth());
            total = total != null ? total : BigDecimal.ZERO;

            trend.add(TrendPoint.builder()
                    .label(target.getMonth().getDisplayName(TextStyle.SHORT,
                            new java.util.Locale("es", "PA")) + " " + target.getYear())
                    .amount(total).month(target.getMonthValue()).year(target.getYear()).build());
        }
        return trend;
    }

    private PredictionResult buildPrediction(Long userId, int year, int month,
            BigDecimal netIncome) {
        List<BigDecimal> history = new ArrayList<>();
        YearMonth ym = YearMonth.of(year, month);

        for (int i = 3; i >= 1; i--) {
            YearMonth target = ym.minusMonths(i);
            BigDecimal total = expenseRepository.sumExpensesByUserAndDateRange(userId,
                    target.atDay(1), target.atEndOfMonth());
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                history.add(total);
            }
        }

        if (history.isEmpty()) {
            return PredictionResult.builder().predictedExpenses(BigDecimal.ZERO)
                    .predictedSavings(netIncome).confidenceLevel("LOW").basedOnMonths(0).build();
        }

        // Promedio ponderado (mes más reciente tiene más peso)
        BigDecimal weighted = BigDecimal.ZERO;
        int totalWeight = 0;
        for (int i = 0; i < history.size(); i++) {
            int weight = i + 1;
            weighted = weighted.add(history.get(i).multiply(BigDecimal.valueOf(weight)));
            totalWeight += weight;
        }
        BigDecimal predicted =
                weighted.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
        BigDecimal predictedSavings = netIncome.subtract(predicted).max(BigDecimal.ZERO);

        String confidence = history.size() >= 3 ? "HIGH" : history.size() == 2 ? "MEDIUM" : "LOW";

        return PredictionResult.builder().predictedExpenses(predicted)
                .predictedSavings(predictedSavings).confidenceLevel(confidence)
                .basedOnMonths(history.size()).build();
    }

    private String[] calcRiskLevel(double savingsRate, BigDecimal expenses, BigDecimal income) {
        if (savingsRate < 0) {
            return new String[] {"HIGH", "⚠️ Estás en déficit. Tus gastos superan tus ingresos."};
        } else if (savingsRate < 10) {
            return new String[] {"HIGH",
                    "🔴 Tasa de ahorro crítica. Menos del 10% de tus ingresos quedan disponibles."};
        } else if (savingsRate < 20) {
            return new String[] {"MEDIUM", "🟡 Tu tasa de ahorro es baja. Intenta llegar al 20%."};
        } else {
            return new String[] {"LOW", "🟢 Buena gestión financiera. Mantén este ritmo."};
        }
    }

    private MonthSummary buildMonthSummary(Long userId, YearMonth ym) {
        BigDecimal totalExpenses = expenseRepository.sumExpensesByUserAndDateRange(userId,
                ym.atDay(1), ym.atEndOfMonth());
        totalExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;

        List<Income> incomes = incomeRepository.findByUserIdAndActiveTrueWithDeductions(userId);
        BigDecimal netIncome = calcNetIncome(incomes);
        BigDecimal balance = netIncome.subtract(totalExpenses);

        return MonthSummary.builder().year(ym.getYear()).month(ym.getMonthValue())
                .totalExpenses(totalExpenses).totalIncome(netIncome).balance(balance).build();
    }

    private Map<String, BigDecimal> expensesByCategoryMap(Long userId, YearMonth ym) {
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(
                userId, ym.atDay(1), ym.atEndOfMonth());

        return expenses.stream().collect(Collectors.groupingBy(e -> e.getCategory().getName(),
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
    }
}
