package com.smartfinancepty.finance.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.graphql.dto.DashboardResponse;
import com.smartfinancepty.finance.service.analytics.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardResolver {

    private final DashboardService dashboardService;

    @QueryMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal User user) {
        return dashboardService.getDashboard(user.getId());
    }

    @QueryMapping
    public DashboardResponse dashboardByMonth(@Argument int year, @Argument int month,
            @AuthenticationPrincipal User user) {
        return dashboardService.getDashboardByMonth(user.getId(), year, month);
    }

}
