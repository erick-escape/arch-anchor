package tcc.com.viewer.dto.recommendations;

import java.io.Serializable;

public record MergeModuleRecommendationDTO(
        String module1Id,
        String module1Name,
        String module2Id,
        String module2Name,
        MergeMetricsDTO metrics
) implements Serializable {
}
