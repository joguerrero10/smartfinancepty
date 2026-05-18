package com.smartfinancepty.finance.domain.notification;


public enum NotificationType {
    EXPENSE_DUE, // Gasto fijo próximo a vencer
    BUDGET_NEAR_LIMIT, // Presupuesto al 80%
    BUDGET_EXCEEDED, // Presupuesto superado
    SAVINGS_GOAL_REMINDER, // Meta de ahorro no alcanzada
    GENERAL // Notificación general
}
