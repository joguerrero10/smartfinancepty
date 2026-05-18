package com.smartfinancepty.finance.service.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.Deduction;
import com.smartfinancepty.finance.domain.Income;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.DeductionResponse;
import com.smartfinancepty.finance.dto.IncomeRequest;
import com.smartfinancepty.finance.dto.IncomeResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.IncomeRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    public List<IncomeResponse> getAllIncomes(Long userId) {
        return incomeRepository.findByUserIdAndActiveTrueWithDeductions(userId).stream()
                .map(this::toResponse).toList();
    }

    public IncomeResponse getIncomeById(Long id, Long userId) {
        Income income = incomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado"));
        return toResponse(income);
    }

    @Transactional
    public IncomeResponse createIncome(IncomeRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Income income = Income.builder().user(user).name(request.getName())
                .amount(request.getAmount()).frequency(request.getFrequency())
                .incomeType(request.getIncomeType()).active(true).build();

        if (request.getDeductions() != null) {
            request.getDeductions().forEach(d -> {
                Deduction deduction = Deduction.builder().income(income).name(d.getName())
                        .deductionType(d.getDeductionType()).isPercentage(d.isPercentage())
                        .value(d.getValue()).build();
                income.getDeductions().add(deduction);
            });
        }

        return toResponse(incomeRepository.save(income));
    }

    @Transactional
    public IncomeResponse updateIncome(Long id, IncomeRequest request, Long userId) {
        Income income = incomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado"));

        income.setName(request.getName());
        income.setAmount(request.getAmount());
        income.setFrequency(request.getFrequency());
        income.setIncomeType(request.getIncomeType());

        return toResponse(incomeRepository.save(income));
    }

    @Transactional
    public void deleteIncome(Long id, Long userId) {
        Income income = incomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado"));
        income.setActive(false);
        incomeRepository.save(income);
    }

    // ── Cálculos de deducciones ──────────────────────────────────────────
    private IncomeResponse toResponse(Income income) {
        List<DeductionResponse> deductionResponses = income.getDeductions().stream().map(d -> {
            BigDecimal calculated =
                    d.isPercentage()
                            ? income.getAmount().multiply(d.getValue())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : d.getValue();

            return DeductionResponse.builder().id(d.getId()).name(d.getName())
                    .deductionType(d.getDeductionType()).isPercentage(d.isPercentage())
                    .value(d.getValue()).calculatedAmount(calculated).build();
        }).toList();

        BigDecimal totalDeductions =
                deductionResponses.stream().map(DeductionResponse::getCalculatedAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netAmount = income.getAmount().subtract(totalDeductions);

        return IncomeResponse.builder().id(income.getId()).name(income.getName())
                .amount(income.getAmount()).netAmount(netAmount).totalDeductions(totalDeductions)
                .frequency(income.getFrequency()).incomeType(income.getIncomeType())
                .active(income.isActive()).deductions(deductionResponses)
                .createdAt(income.getCreatedAt()).build();
    }
}
