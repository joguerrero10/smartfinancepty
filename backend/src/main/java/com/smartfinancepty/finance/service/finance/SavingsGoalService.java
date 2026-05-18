package com.smartfinancepty.finance.service.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.SavingsGoal;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.SavingsGoalRequest;
import com.smartfinancepty.finance.dto.SavingsGoalResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.IncomeRepository;
import com.smartfinancepty.finance.repository.SavingsGoalRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getAllGoals(Long userId) {
        LocalDate now = LocalDate.now();
        return savingsGoalRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(g -> toResponse(g, userId, now.getYear(), now.getMonthValue())).toList();
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getGoalsByMonth(Long userId, int year, int month) {
        return savingsGoalRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(g -> toResponse(g, userId, year, month)).toList();
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getGoalById(Long id, Long userId) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta de ahorro no encontrada"));
        LocalDate now = LocalDate.now();
        return toResponse(goal, userId, now.getYear(), now.getMonthValue());
    }

    @Transactional
    public SavingsGoalResponse createGoal(SavingsGoalRequest request, Long userId) {
        if (request.getFixedAmount() == null && request.getPercentage() == null) {
            throw new IllegalArgumentException(
                    "Debe especificar un monto fijo o un porcentaje para la meta de ahorro");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        SavingsGoal goal = SavingsGoal.builder().user(user).name(request.getName())
                .fixedAmount(request.getFixedAmount()).percentage(request.getPercentage())
                .active(true).build();

        SavingsGoal saved = savingsGoalRepository.save(goal);
        LocalDate now = LocalDate.now();
        return toResponse(saved, userId, now.getYear(), now.getMonthValue());
    }

    @Transactional
    public SavingsGoalResponse updateGoal(Long id, SavingsGoalRequest request, Long userId) {
        if (request.getFixedAmount() == null && request.getPercentage() == null) {
            throw new IllegalArgumentException("Debe especificar un monto fijo o un porcentaje");
        }

        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta de ahorro no encontrada"));

        goal.setName(request.getName());
        goal.setFixedAmount(request.getFixedAmount());
        goal.setPercentage(request.getPercentage());

        SavingsGoal saved = savingsGoalRepository.save(goal);
        LocalDate now = LocalDate.now();
        return toResponse(saved, userId, now.getYear(), now.getMonthValue());
    }

    @Transactional
    public void deleteGoal(Long id, Long userId) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta de ahorro no encontrada"));
        goal.setActive(false);
        savingsGoalRepository.save(goal);
    }

    // ── Cálculo de meta y estado ─────────────────────────────────────────────
    private SavingsGoalResponse toResponse(SavingsGoal goal, Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // Ingreso neto del mes
        List<com.smartfinancepty.finance.domain.Income> incomes =
                incomeRepository.findByUserIdAndActiveTrueWithDeductions(userId);

        BigDecimal netIncome = incomes.stream().map(income -> {
            BigDecimal totalDed = income.getDeductions().stream()
                    .map(d -> d.isPercentage()
                            ? income.getAmount().multiply(d.getValue())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : d.getValue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return income.getAmount().subtract(totalDed);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gasto total del mes
        BigDecimal totalExpenses =
                expenseRepository.sumExpensesByUserAndDateRange(userId, start, end);
        if (totalExpenses == null)
            totalExpenses = BigDecimal.ZERO;

        // Ahorro real = ingreso neto - gastos
        BigDecimal actualSavings = netIncome.subtract(totalExpenses).max(BigDecimal.ZERO);

        // Meta objetivo
        BigDecimal targetAmount;
        if (goal.getFixedAmount() != null && goal.getPercentage() != null) {
            // Ambos definidos: usa el mayor
            BigDecimal byPercentage = netIncome.multiply(goal.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            targetAmount = goal.getFixedAmount().max(byPercentage);
        } else if (goal.getFixedAmount() != null) {
            targetAmount = goal.getFixedAmount();
        } else {
            targetAmount = netIncome.multiply(goal.getPercentage()).divide(BigDecimal.valueOf(100),
                    2, RoundingMode.HALF_UP);
        }

        boolean isAchieved = actualSavings.compareTo(targetAmount) >= 0;

        double achievementPct = targetAmount.compareTo(BigDecimal.ZERO) > 0 ? actualSavings
                .divide(targetAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        String statusMessage;
        if (isAchieved) {
            statusMessage = "✅ ¡Meta alcanzada! Ahorraste $"
                    + actualSavings.setScale(2, RoundingMode.HALF_UP) + " de $"
                    + targetAmount.setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal diff = targetAmount.subtract(actualSavings);
            statusMessage = "📊 Te faltan $" + diff.setScale(2, RoundingMode.HALF_UP)
                    + " para alcanzar tu meta de $"
                    + targetAmount.setScale(2, RoundingMode.HALF_UP);
        }

        return SavingsGoalResponse.builder().id(goal.getId()).name(goal.getName())
                .fixedAmount(goal.getFixedAmount()).percentage(goal.getPercentage())
                .targetAmount(targetAmount).actualSavings(actualSavings).isAchieved(isAchieved)
                .achievementPercentage(Math.round(achievementPct * 100.0) / 100.0)
                .statusMessage(statusMessage).build();
    }
}
