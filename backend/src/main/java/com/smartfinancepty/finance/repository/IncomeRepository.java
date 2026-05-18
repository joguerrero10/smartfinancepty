package com.smartfinancepty.finance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.Income;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    @Query("SELECT i FROM Income i LEFT JOIN FETCH i.deductions WHERE i.user.id = :userId AND i.active = true")
    List<Income> findByUserIdAndActiveTrueWithDeductions(Long userId);

    Optional<Income> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId AND i.active = true")
    java.math.BigDecimal sumActiveIncomeByUserId(Long userId);
}
