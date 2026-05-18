package com.smartfinancepty.finance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndYearAndMonthAndActiveTrue(Long userId, int year, int month);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    // Presupuesto global (sin categoría)
    Optional<Budget> findByUserIdAndCategoryIsNullAndYearAndMonthAndActiveTrue(Long userId,
            int year, int month);

    // Presupuesto por categoría
    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonthAndActiveTrue(Long userId,
            Long categoryId, int year, int month);

    // Todos los presupuestos activos del usuario
    List<Budget> findByUserIdAndActiveTrue(Long userId);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.category "
            + "WHERE b.user.id = :userId AND b.year = :year AND b.month = :month AND b.active = true")
    List<Budget> findByUserIdAndPeriodWithCategory(Long userId, int year, int month);
}
