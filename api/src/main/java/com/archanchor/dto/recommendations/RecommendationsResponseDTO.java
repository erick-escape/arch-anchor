package com.archanchor.dto.recommendations;

import java.io.Serializable;
import java.util.List;

public record RecommendationsResponseDTO(List<SplitModuleRecommendationDTO> splits,
		List<MergeModuleRecommendationDTO> merges, List<MoveClassRecommendationDTO> moves,
		List<ArchitectureViolationRecommendationDTO> violations,
		RecommendationsSummaryDTO summary) implements Serializable {
}
