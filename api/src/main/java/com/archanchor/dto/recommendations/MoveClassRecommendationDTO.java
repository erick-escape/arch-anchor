package com.archanchor.dto.recommendations;

import java.io.Serializable;

public record MoveClassRecommendationDTO(String classId, String className, String sourceModuleId,
		String sourceModuleName, String targetModuleId, String targetModuleName,
		MoveMetricsDTO metrics) implements Serializable {
}
