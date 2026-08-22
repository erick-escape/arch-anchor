package com.archanchor.dto.recommendations;

import java.io.Serializable;
import java.util.List;

public record SplitModuleRecommendationDTO(String originalModuleId, String originalModuleName, String superRefClassName,
		String deRefClassName, List<String> module1ClassIds, List<String> module2ClassIds,
		SplitMetricsDTO metrics) implements Serializable {
}
