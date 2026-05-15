package com.smartfinancepty.finance.service.finance;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.*;
import com.smartfinancepty.finance.dto.ExpenseRequest;
import com.smartfinancepty.finance.dto.ExpenseResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.ExpenseCategoryRepository;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.ExpenseSubCategoryRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseSubCategoryRepository subCategoryRepository;

    public List<ExpenseResponse> getAllExpenses(Long userId) {
        return expenseRepository.findByUserIdAndActiveTrue(userId).stream().map(this::toResponse)
                .toList();
    }

    public List<ExpenseResponse> getExpensesByDateRange(Long userId, LocalDate start,
            LocalDate end) {
        return expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(userId, start, end)
                .stream().map(this::toResponse).toList();
    }

    public List<ExpenseResponse> getExpensesByType(Long userId, ExpenseType type) {
        return expenseRepository.findByUserIdAndExpenseTypeAndActiveTrue(userId, type).stream()
                .map(this::toResponse).toList();
    }

    public ExpenseResponse getExpenseById(Long id, Long userId) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado"));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        ExpenseSubCategory subCategory = null;
        if (request.getSubCategoryId() != null) {
            subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subcategoría no encontrada"));
        }

        Expense expense = Expense.builder().user(user).category(category).subCategory(subCategory)
                .description(request.getDescription()).amount(request.getAmount())
                .expenseType(request.getExpenseType()).expenseDate(request.getExpenseDate())
                .frequency(request.getFrequency()).dueDay(request.getDueDay())
                .notes(request.getNotes()).active(true).build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long userId) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado"));

        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        expense.setCategory(category);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setExpenseType(request.getExpenseType());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setFrequency(request.getFrequency());
        expense.setDueDay(request.getDueDay());
        expense.setNotes(request.getNotes());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(Long id, Long userId) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado"));
        expense.setActive(false);
        expenseRepository.save(expense);
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder().id(expense.getId()).description(expense.getDescription())
                .amount(expense.getAmount()).expenseType(expense.getExpenseType())
                .categoryName(expense.getCategory().getName())
                .subCategoryName(
                        expense.getSubCategory() != null ? expense.getSubCategory().getName()
                                : null)
                .expenseDate(expense.getExpenseDate()).frequency(expense.getFrequency())
                .dueDay(expense.getDueDay()).notes(expense.getNotes()).active(expense.isActive())
                .createdAt(expense.getCreatedAt()).build();
    }
}
