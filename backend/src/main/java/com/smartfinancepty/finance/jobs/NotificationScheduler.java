package com.smartfinancepty.finance.jobs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.*;
import com.smartfinancepty.finance.domain.notification.NotificationChannel;
import com.smartfinancepty.finance.domain.notification.NotificationType;
import com.smartfinancepty.finance.domain.notification.Reminder;
import com.smartfinancepty.finance.repository.BudgetRepository;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.IncomeRepository;
import com.smartfinancepty.finance.repository.SavingsGoalRepository;
import com.smartfinancepty.finance.repository.notification.ReminderRepository;
import com.smartfinancepty.finance.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final ReminderRepository reminderRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final IncomeRepository incomeRepository;
    private final NotificationService notificationService;

    /**
     * Cada día a las 8:00 AM — recordatorios de gastos fijos Alerta si un gasto vence HOY o en los
     * próximos 3 días
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkExpireDueReminders() {
        log.info("⏰ Cron: verificando gastos próximos a vencer...");

        int today = LocalDate.now().getDayOfMonth();

        // Gastos que vencen hoy
        List<Expense> dueToday = expenseRepository.findAll().stream()
                .filter(e -> e.isActive() && e.getExpenseType() == ExpenseType.FIXED
                        && e.getDueDay() != null && e.getDueDay() == today)
                .toList();

        for (Expense expense : dueToday) {
            notificationService.sendNotification(expense.getUser(),
                    "📅 Pago de hoy: " + expense.getDescription(),
                    "Hoy debes pagar $" + expense.getAmount() + " por " + expense.getDescription(),
                    NotificationType.EXPENSE_DUE, NotificationChannel.BOTH, expense.getId(),
                    "EXPENSE");
        }

        // Gastos que vencen en 3 días
        int in3Days = LocalDate.now().plusDays(3).getDayOfMonth();
        List<Expense> dueSoon = expenseRepository.findAll().stream()
                .filter(e -> e.isActive() && e.getExpenseType() == ExpenseType.FIXED
                        && e.getDueDay() != null && e.getDueDay() == in3Days)
                .toList();

        for (Expense expense : dueSoon) {
            notificationService.sendNotification(expense.getUser(),
                    "🔔 En 3 días: " + expense.getDescription(),
                    "El " + in3Days + " vence el pago de $" + expense.getAmount() + " por "
                            + expense.getDescription(),
                    NotificationType.EXPENSE_DUE, NotificationChannel.BOTH, expense.getId(),
                    "EXPENSE");
        }

        log.info("⏰ Cron gastos: {} vencen hoy, {} vencen en 3 días", dueToday.size(),
                dueSoon.size());
    }

    /**
     * Cada día a las 9:00 AM — alertas de presupuesto
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkBudgetAlerts() {
        log.info("⏰ Cron: verificando alertas de presupuesto...");

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        List<Budget> budgets =
                budgetRepository.findByUserIdAndYearAndMonthAndActiveTrue(null, year, month);

        // Obtener todos los presupuestos del mes actual
        List<Budget> allBudgets = budgetRepository.findAll().stream()
                .filter(b -> b.isActive() && b.getYear() == year && b.getMonth() == month).toList();

        for (Budget budget : allBudgets) {
            LocalDate start = YearMonth.of(year, month).atDay(1);
            LocalDate end = YearMonth.of(year, month).atEndOfMonth();

            BigDecimal spent;
            if (budget.getCategory() == null) {
                BigDecimal total = expenseRepository
                        .sumExpensesByUserAndDateRange(budget.getUser().getId(), start, end);
                spent = total != null ? total : BigDecimal.ZERO;
            } else {
                BigDecimal total = expenseRepository.sumExpensesByCategory(budget.getUser().getId(),
                        budget.getCategory().getId(), start, end);
                spent = total != null ? total : BigDecimal.ZERO;
            }

            if (budget.getLimitAmount().compareTo(BigDecimal.ZERO) == 0)
                continue;

            double pct = spent.doubleValue() / budget.getLimitAmount().doubleValue() * 100;
            String categoryName =
                    budget.getCategory() != null ? budget.getCategory().getName() : "global";

            if (pct > 100) {
                BigDecimal excess = spent.subtract(budget.getLimitAmount());
                notificationService.sendNotification(budget.getUser(),
                        "⚠️ Presupuesto " + categoryName + " superado",
                        "Superaste tu presupuesto de " + categoryName + " por $"
                                + excess.setScale(2, java.math.RoundingMode.HALF_UP),
                        NotificationType.BUDGET_EXCEEDED, NotificationChannel.BOTH, budget.getId(),
                        "BUDGET");
            } else if (pct >= 80) {
                notificationService.sendNotification(budget.getUser(),
                        "🔔 Presupuesto " + categoryName + " al " + String.format("%.0f", pct)
                                + "%",
                        "Llevas el " + String.format("%.1f", pct) + "% de tu presupuesto de "
                                + categoryName + ". Quedan $"
                                + budget.getLimitAmount().subtract(spent).setScale(2,
                                        java.math.RoundingMode.HALF_UP),
                        NotificationType.BUDGET_NEAR_LIMIT, NotificationChannel.BOTH,
                        budget.getId(), "BUDGET");
            }
        }

        log.info("⏰ Cron presupuestos: {} revisados", allBudgets.size());
    }

    /**
     * El último día del mes a las 6:00 PM — metas de ahorro no alcanzadas
     */
    @Scheduled(cron = "0 0 18 28-31 * *")
    @Transactional
    public void checkSavingsGoalReminders() {
        LocalDate today = LocalDate.now();
        YearMonth current = YearMonth.of(today.getYear(), today.getMonthValue());

        // Solo ejecutar el último día del mes
        if (today.getDayOfMonth() != current.lengthOfMonth())
            return;

        log.info("⏰ Cron: verificando metas de ahorro del mes...");

        List<SavingsGoal> goals =
                savingsGoalRepository.findAll().stream().filter(SavingsGoal::isActive).toList();

        for (SavingsGoal goal : goals) {
            Long userId = goal.getUser().getId();
            LocalDate start = current.atDay(1);
            LocalDate end = current.atEndOfMonth();

            List<Income> incomes = incomeRepository.findByUserIdAndActiveTrueWithDeductions(userId);

            BigDecimal netIncome = incomes.stream().map(income -> {
                BigDecimal totalDed = income.getDeductions().stream()
                        .map(d -> d.isPercentage()
                                ? income.getAmount().multiply(d.getValue()).divide(
                                        BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                                : d.getValue())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return income.getAmount().subtract(totalDed);
            }).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpenses =
                    expenseRepository.sumExpensesByUserAndDateRange(userId, start, end);
            if (totalExpenses == null)
                totalExpenses = BigDecimal.ZERO;

            BigDecimal actualSavings = netIncome.subtract(totalExpenses).max(BigDecimal.ZERO);

            BigDecimal target = goal.getFixedAmount() != null ? goal.getFixedAmount()
                    : netIncome.multiply(goal.getPercentage()).divide(BigDecimal.valueOf(100), 2,
                            java.math.RoundingMode.HALF_UP);

            if (actualSavings.compareTo(target) < 0) {
                BigDecimal diff = target.subtract(actualSavings);
                notificationService.sendNotification(goal.getUser(),
                        "📊 Meta de ahorro: " + goal.getName(),
                        "Este mes ahorraste $"
                                + actualSavings.setScale(2, java.math.RoundingMode.HALF_UP)
                                + " de tu meta de $"
                                + target.setScale(2, java.math.RoundingMode.HALF_UP)
                                + ". Te faltaron $"
                                + diff.setScale(2, java.math.RoundingMode.HALF_UP),
                        NotificationType.SAVINGS_GOAL_REMINDER, NotificationChannel.BOTH,
                        goal.getId(), "SAVINGS_GOAL");
            }
        }

        log.info("⏰ Cron metas: {} revisadas", goals.size());
    }

    /**
     * Cada hora — recordatorios puntuales del usuario
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkPunctualReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        List<Reminder> pending = reminderRepository
                .findByActiveTrueAndRecurringFalseAndRemindAtBetween(now, oneHourLater);

        for (Reminder reminder : pending) {
            notificationService.sendNotification(reminder.getUser(), reminder.getTitle(),
                    reminder.getMessage() != null ? reminder.getMessage() : reminder.getTitle(),
                    NotificationType.GENERAL, reminder.getChannel(), reminder.getId(), "REMINDER");
            reminder.setActive(false);
            reminderRepository.save(reminder);
        }

        if (!pending.isEmpty()) {
            log.info("⏰ Cron recordatorios puntuales: {} enviados", pending.size());
        }
    }

    /**
     * Cada día a las 8:00 AM — recordatorios recurrentes del usuario
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkRecurringReminders() {
        int today = LocalDate.now().getDayOfMonth();
        List<Reminder> reminders = reminderRepository.findByActiveTrueAndDayOfMonth(today);

        for (Reminder reminder : reminders) {
            notificationService.sendNotification(reminder.getUser(), reminder.getTitle(),
                    reminder.getMessage() != null ? reminder.getMessage() : reminder.getTitle(),
                    NotificationType.GENERAL, reminder.getChannel(), reminder.getId(), "REMINDER");
        }

        if (!reminders.isEmpty()) {
            log.info("⏰ Cron recordatorios recurrentes: {} enviados", reminders.size());
        }
    }
}
