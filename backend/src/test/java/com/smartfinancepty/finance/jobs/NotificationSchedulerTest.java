package com.smartfinancepty.finance.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationScheduler Tests")
class NotificationSchedulerTest {

    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private SavingsGoalRepository savingsGoalRepository;
    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    private User testUser;
    private ExpenseCategory testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com")
                .username("joel@smartfinance.com").fullName("Joel Guerrero").role(Role.USER)
                .enabled(true).build();

        testCategory = ExpenseCategory.builder().id(1L).name("Servicios").build();
    }

    @Nested
    @DisplayName("checkExpireDueReminders")
    class CheckExpireDueRemindersTests {

        @Test
        @DisplayName("Debe enviar notificación para gastos que vencen hoy")
        void shouldSendNotificationForExpensesDueToday() {
            int today = LocalDate.now().getDayOfMonth();

            Expense dueExpense = Expense.builder().id(1L).user(testUser).category(testCategory)
                    .description("Electricidad").amount(new BigDecimal("45.00"))
                    .expenseType(ExpenseType.FIXED).expenseDate(LocalDate.now())
                    .dueDay(today).active(true).build();

            when(expenseRepository.findAll()).thenReturn(List.of(dueExpense));

            notificationScheduler.checkExpireDueReminders();

            verify(notificationService).sendNotification(eq(testUser), anyString(), anyString(),
                    eq(NotificationType.EXPENSE_DUE), eq(NotificationChannel.BOTH),
                    eq(1L), eq("EXPENSE"));
        }

        @Test
        @DisplayName("Debe enviar notificación para gastos que vencen en 3 días")
        void shouldSendNotificationForExpensesDueIn3Days() {
            int in3Days = LocalDate.now().plusDays(3).getDayOfMonth();

            Expense dueSoonExpense = Expense.builder().id(2L).user(testUser).category(testCategory)
                    .description("Agua").amount(new BigDecimal("20.00"))
                    .expenseType(ExpenseType.FIXED).expenseDate(LocalDate.now())
                    .dueDay(in3Days).active(true).build();

            when(expenseRepository.findAll()).thenReturn(List.of(dueSoonExpense));

            notificationScheduler.checkExpireDueReminders();

            verify(notificationService).sendNotification(eq(testUser), anyString(), anyString(),
                    eq(NotificationType.EXPENSE_DUE), eq(NotificationChannel.BOTH),
                    eq(2L), eq("EXPENSE"));
        }

        @Test
        @DisplayName("No debe enviar notificación si no hay gastos por vencer")
        void shouldNotSendNotificationWhenNoExpensesDue() {
            Expense irrelevantExpense = Expense.builder().id(3L).user(testUser).category(testCategory)
                    .description("Otro").amount(new BigDecimal("10.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now())
                    .active(true).build();

            when(expenseRepository.findAll()).thenReturn(List.of(irrelevantExpense));

            notificationScheduler.checkExpireDueReminders();

            verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(),
                    any(), any());
        }

        @Test
        @DisplayName("Debe ignorar gastos inactivos")
        void shouldIgnoreInactiveExpenses() {
            int today = LocalDate.now().getDayOfMonth();

            Expense inactiveExpense = Expense.builder().id(4L).user(testUser).category(testCategory)
                    .description("Inactivo").amount(new BigDecimal("30.00"))
                    .expenseType(ExpenseType.FIXED).expenseDate(LocalDate.now())
                    .dueDay(today).active(false).build();

            when(expenseRepository.findAll()).thenReturn(List.of(inactiveExpense));

            notificationScheduler.checkExpireDueReminders();

            verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(),
                    any(), any());
        }
    }

    @Nested
    @DisplayName("checkBudgetAlerts")
    class CheckBudgetAlertsTests {

        @Test
        @DisplayName("Debe enviar alerta de presupuesto superado")
        void shouldSendBudgetExceededAlert() {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            Budget budget = Budget.builder().id(1L).user(testUser)
                    .limitAmount(new BigDecimal("200.00")).year(year).month(month).active(true).build();

            when(budgetRepository.findByUserIdAndYearAndMonthAndActiveTrue(any(), eq(year), eq(month)))
                    .thenReturn(List.of());
            when(budgetRepository.findAll()).thenReturn(List.of(budget));
            // Gastado 250 > límite 200 → superado
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("250.00"));

            notificationScheduler.checkBudgetAlerts();

            verify(notificationService).sendNotification(eq(testUser), anyString(), anyString(),
                    eq(NotificationType.BUDGET_EXCEEDED), any(), eq(1L), eq("BUDGET"));
        }

        @Test
        @DisplayName("Debe enviar alerta de presupuesto al 80%")
        void shouldSendBudgetNearLimitAlert() {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            Budget budget = Budget.builder().id(2L).user(testUser)
                    .limitAmount(new BigDecimal("200.00")).year(year).month(month).active(true).build();

            when(budgetRepository.findByUserIdAndYearAndMonthAndActiveTrue(any(), eq(year), eq(month)))
                    .thenReturn(List.of());
            when(budgetRepository.findAll()).thenReturn(List.of(budget));
            // Gastado 170 = 85% de 200
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("170.00"));

            notificationScheduler.checkBudgetAlerts();

            verify(notificationService).sendNotification(eq(testUser), anyString(), anyString(),
                    eq(NotificationType.BUDGET_NEAR_LIMIT), any(), eq(2L), eq("BUDGET"));
        }

        @Test
        @DisplayName("Debe enviar alerta para presupuesto por categoría")
        void shouldSendCategoryBudgetAlert() {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            Budget categoryBudget = Budget.builder().id(3L).user(testUser)
                    .category(testCategory).limitAmount(new BigDecimal("100.00"))
                    .year(year).month(month).active(true).build();

            when(budgetRepository.findByUserIdAndYearAndMonthAndActiveTrue(any(), eq(year), eq(month)))
                    .thenReturn(List.of());
            when(budgetRepository.findAll()).thenReturn(List.of(categoryBudget));
            when(expenseRepository.sumExpensesByCategory(eq(1L), eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("90.00")); // 90% del límite

            notificationScheduler.checkBudgetAlerts();

            verify(notificationService).sendNotification(eq(testUser), anyString(), anyString(),
                    eq(NotificationType.BUDGET_NEAR_LIMIT), any(), eq(3L), eq("BUDGET"));
        }

        @Test
        @DisplayName("No debe enviar alerta si gasto es bajo")
        void shouldNotSendAlertWhenSpentIsLow() {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            Budget budget = Budget.builder().id(4L).user(testUser)
                    .limitAmount(new BigDecimal("500.00")).year(year).month(month).active(true).build();

            when(budgetRepository.findByUserIdAndYearAndMonthAndActiveTrue(any(), eq(year), eq(month)))
                    .thenReturn(List.of());
            when(budgetRepository.findAll()).thenReturn(List.of(budget));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("100.00")); // 20% del límite

            notificationScheduler.checkBudgetAlerts();

            verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(),
                    any(), any());
        }

        @Test
        @DisplayName("Debe ignorar presupuesto con límite cero")
        void shouldIgnoreBudgetWithZeroLimit() {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            Budget zeroBudget = Budget.builder().id(5L).user(testUser)
                    .limitAmount(BigDecimal.ZERO).year(year).month(month).active(true).build();

            when(budgetRepository.findByUserIdAndYearAndMonthAndActiveTrue(any(), eq(year), eq(month)))
                    .thenReturn(List.of());
            when(budgetRepository.findAll()).thenReturn(List.of(zeroBudget));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            notificationScheduler.checkBudgetAlerts();

            verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(),
                    any(), any());
        }
    }

    @Nested
    @DisplayName("checkSavingsGoalReminders")
    class CheckSavingsGoalRemindersTests {

        @Test
        @DisplayName("Debe enviar recordatorio de meta no alcanzada al final del mes")
        void shouldSendReminderWhenGoalNotMet() {
            // Sólo ejecuta si hoy es el último día del mes
            YearMonth current = YearMonth.now();
            if (LocalDate.now().getDayOfMonth() != current.lengthOfMonth()) {
                return; // Skip test if not last day of month
            }

            Income income = Income.builder().id(1L).user(testUser).name("Salario")
                    .amount(new BigDecimal("1000.00")).frequency(FrequencyType.MONTHLY)
                    .incomeType(IncomeType.SALARY).deductions(List.of()).active(true).build();

            SavingsGoal goal = SavingsGoal.builder().id(1L).user(testUser).name("Fondo de emergencia")
                    .fixedAmount(new BigDecimal("300.00")).active(true).build();

            when(savingsGoalRepository.findAll()).thenReturn(List.of(goal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(income));
            // Gastos = 800, ingreso = 1000, ahorro = 200 < meta 300 → alerta
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("800.00"));

            notificationScheduler.checkSavingsGoalReminders();

            verify(notificationService).sendNotification(eq(testUser), anyString(), anyString(),
                    eq(NotificationType.SAVINGS_GOAL_REMINDER), any(), eq(1L), eq("SAVINGS_GOAL"));
        }

        @Test
        @DisplayName("No debe ejecutar si no es el último día del mes")
        void shouldNotExecuteIfNotLastDayOfMonth() {
            YearMonth current = YearMonth.now();
            if (LocalDate.now().getDayOfMonth() == current.lengthOfMonth()) {
                return; // Skip if last day of month
            }

            notificationScheduler.checkSavingsGoalReminders();

            verify(savingsGoalRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("checkPunctualReminders")
    class CheckPunctualRemindersTests {

        @Test
        @DisplayName("Debe enviar recordatorio puntual y desactivarlo")
        void shouldSendPunctualReminderAndDeactivate() {
            LocalDateTime now = LocalDateTime.now();

            Reminder reminder = Reminder.builder().id(1L).user(testUser).title("Pagar deuda")
                    .message("Recuerda pagar la deuda").channel(NotificationChannel.PUSH)
                    .recurring(false).remindAt(now.plusMinutes(30)).active(true).build();

            when(reminderRepository.findByActiveTrueAndRecurringFalseAndRemindAtBetween(any(), any()))
                    .thenReturn(List.of(reminder));
            when(reminderRepository.save(any())).thenReturn(reminder);

            notificationScheduler.checkPunctualReminders();

            verify(notificationService).sendNotification(eq(testUser), eq("Pagar deuda"),
                    eq("Recuerda pagar la deuda"), eq(NotificationType.GENERAL),
                    eq(NotificationChannel.PUSH), eq(1L), eq("REMINDER"));
            assertThat(reminder.isActive()).isFalse();
            verify(reminderRepository).save(reminder);
        }

        @Test
        @DisplayName("Debe usar título como mensaje si message es null")
        void shouldUseTitleWhenMessageIsNull() {
            Reminder reminder = Reminder.builder().id(2L).user(testUser).title("Recordatorio sin msg")
                    .message(null).channel(NotificationChannel.EMAIL)
                    .recurring(false).remindAt(LocalDateTime.now().plusMinutes(10)).active(true)
                    .build();

            when(reminderRepository.findByActiveTrueAndRecurringFalseAndRemindAtBetween(any(), any()))
                    .thenReturn(List.of(reminder));
            when(reminderRepository.save(any())).thenReturn(reminder);

            notificationScheduler.checkPunctualReminders();

            verify(notificationService).sendNotification(eq(testUser), anyString(),
                    eq("Recordatorio sin msg"), any(), any(), any(), any());
        }

        @Test
        @DisplayName("No debe hacer nada si no hay recordatorios pendientes")
        void shouldDoNothingWhenNoPendingReminders() {
            when(reminderRepository.findByActiveTrueAndRecurringFalseAndRemindAtBetween(any(), any()))
                    .thenReturn(List.of());

            notificationScheduler.checkPunctualReminders();

            verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(),
                    any(), any());
        }
    }

    @Nested
    @DisplayName("checkRecurringReminders")
    class CheckRecurringRemindersTests {

        @Test
        @DisplayName("Debe enviar recordatorios recurrentes del día")
        void shouldSendRecurringReminders() {
            int today = LocalDate.now().getDayOfMonth();

            Reminder reminder = Reminder.builder().id(1L).user(testUser).title("Pago mensual")
                    .message("Haz tu pago").channel(NotificationChannel.EMAIL)
                    .recurring(true).dayOfMonth(today).active(true).build();

            when(reminderRepository.findByActiveTrueAndDayOfMonth(today))
                    .thenReturn(List.of(reminder));

            notificationScheduler.checkRecurringReminders();

            verify(notificationService).sendNotification(eq(testUser), eq("Pago mensual"),
                    eq("Haz tu pago"), eq(NotificationType.GENERAL), eq(NotificationChannel.EMAIL),
                    eq(1L), eq("REMINDER"));
        }

        @Test
        @DisplayName("Debe usar título si message es null en recurrente")
        void shouldUseTitleWhenMessageIsNullInRecurring() {
            int today = LocalDate.now().getDayOfMonth();

            Reminder reminder = Reminder.builder().id(2L).user(testUser).title("Sin mensaje")
                    .message(null).channel(NotificationChannel.PUSH)
                    .recurring(true).dayOfMonth(today).active(true).build();

            when(reminderRepository.findByActiveTrueAndDayOfMonth(today))
                    .thenReturn(List.of(reminder));

            notificationScheduler.checkRecurringReminders();

            verify(notificationService).sendNotification(eq(testUser), eq("Sin mensaje"),
                    eq("Sin mensaje"), any(), any(), any(), any());
        }

        @Test
        @DisplayName("No debe hacer nada si no hay recordatorios recurrentes hoy")
        void shouldDoNothingWhenNoRecurringReminders() {
            int today = LocalDate.now().getDayOfMonth();
            when(reminderRepository.findByActiveTrueAndDayOfMonth(today)).thenReturn(List.of());

            notificationScheduler.checkRecurringReminders();

            verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(),
                    any(), any());
        }
    }

}
