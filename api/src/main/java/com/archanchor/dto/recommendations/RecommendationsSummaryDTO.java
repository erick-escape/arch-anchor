package com.archanchor.dto.recommendations;

import java.io.Serializable;

public record RecommendationsSummaryDTO(int totalRecommendations, int highPriority, int mediumPriority,
		int lowPriority) implements Serializable {
}
