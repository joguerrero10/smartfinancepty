package com.smartfinancepty.finance.graphql.resolver;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.graphql.dto.AnalyticsResponse;
import com.smartfinancepty.finance.graphql.dto.ComparisonResponse;
import com.smartfinancepty.finance.graphql.dto.Recommendation;
import com.smartfinancepty.finance.service.analytics.recommendation.AnalyticsService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AnalyticsResolver {

    private final AnalyticsService analyticsService;

    @QueryMapping
    public AnalyticsResponse analytics(@Argument int year, @Argument int month,
            @AuthenticationPrincipal User user) {
        return analyticsService.getAnalytics(user.getId(), year, month);
    }

    @QueryMapping
    public ComparisonResponse analyticsComparison(@Argument int year, @Argument int month,
            @AuthenticationPrincipal User user) {
        return analyticsService.getComparison(user.getId(), year, month);
    }

    @QueryMapping
    public List<Recommendation> recommendations(@AuthenticationPrincipal User user) {
        return analyticsService.getRecommendations(user.getId());
    }

}
