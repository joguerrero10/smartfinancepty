package com.smartfinancepty.finance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.Expense;
import com.smartfinancepty.finance.domain.ExpenseType;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdAndActiveTrue(Long userId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    List<Expense> findByUserIdAndExpenseDateBetweenAndActiveTrue(Long userId, LocalDate start,
            LocalDate end);

    List<Expense> findByUserIdAndExpenseTypeAndActiveTrue(Long userId, ExpenseType type);

    List<Expense> findByUserIdAndCategoryIdAndActiveTrue(Long userId, Long categoryId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId "
            + "AND e.expenseDate BETWEEN :start AND :end AND e.active = true")
    BigDecimal sumExpensesByUserAndDateRange(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId "
            + "AND e.category.id = :categoryId AND e.expenseDate BETWEEN :start AND :end AND e.active = true")
    BigDecimal sumExpensesByCategory(Long userId, Long categoryId, LocalDate start, LocalDate end);
}
