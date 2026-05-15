package com.smartfinancepty.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deductions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_id", nullable = false)
    private Income income;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionType deductionType;

    // Si es porcentaje (true) o monto fijo (false)
    @Column(nullable = false)
    @Builder.Default
    private boolean isPercentage = true;

    // Puede ser porcentaje (ej: 9.75) o monto fijo (ej: 150.00)
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
