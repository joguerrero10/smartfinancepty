package com.smartfinancepty.finance.dto.analytic;

import java.util.List;
import com.smartfinancepty.finance.graphql.dto.AnalyticsResponse;
import com.smartfinancepty.finance.graphql.dto.ComparisonResponse;
import com.smartfinancepty.finance.graphql.dto.Recommendation;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsRestResponse {
    private AnalyticsResponse analytics;
    private List<Recommendation> recommendations;
    private ComparisonResponse comparison;
}
