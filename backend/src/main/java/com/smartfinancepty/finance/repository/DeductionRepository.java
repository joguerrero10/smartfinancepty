package com.smartfinancepty.finance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.Deduction;

@Repository
public interface DeductionRepository extends JpaRepository<Deduction, Long> {
    List<Deduction> findByIncomeId(Long incomeId);

    void deleteByIncomeId(Long incomeId);
}
