package com.smartfinancepty.finance.controllers.analytic;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.analytic.AnalyticsRestResponse;
import com.smartfinancepty.finance.graphql.dto.ComparisonResponse;
import com.smartfinancepty.finance.graphql.dto.Recommendation;
import com.smartfinancepty.finance.service.analytics.recommendation.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Análisis financiero e IA básica")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @Operation(
            summary = "Analytics completo del mes actual (analytics + recomendaciones + comparación)")
    public ResponseEntity<AnalyticsRestResponse> getCurrentAnalytics(
            @AuthenticationPrincipal User user) {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(analyticsService.getAnalyticsRest(user.getId(), now.getYear(),
                now.getMonthValue()));
    }

    @GetMapping("/month")
    @Operation(summary = "Analytics de un mes específico")
    public ResponseEntity<AnalyticsRestResponse> getAnalyticsByMonth(@RequestParam int year,
            @RequestParam int month, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getAnalyticsRest(user.getId(), year, month));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Recomendaciones IA basadas en patrones de gasto")
    public ResponseEntity<List<Recommendation>> getRecommendations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getRecommendations(user.getId()));
    }

    @GetMapping("/comparison")
    @Operation(summary = "Comparación mes actual vs mes anterior")
    public ResponseEntity<ComparisonResponse> getComparison(@RequestParam int year,
            @RequestParam int month, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getComparison(user.getId(), year, month));
    }

}
