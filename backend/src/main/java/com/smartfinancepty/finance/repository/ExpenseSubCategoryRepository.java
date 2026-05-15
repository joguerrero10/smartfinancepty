package com.smartfinancepty.finance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.ExpenseSubCategory;

@Repository
public interface ExpenseSubCategoryRepository extends JpaRepository<ExpenseSubCategory, Long> {
    List<ExpenseSubCategory> findByCategoryId(Long categoryId);

    boolean existsByNameAndCategoryId(String name, Long categoryId);
}
