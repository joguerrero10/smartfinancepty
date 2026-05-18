package com.smartfinancepty.finance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.SavingsGoal;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserIdAndActiveTrue(Long userId);

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);
}
