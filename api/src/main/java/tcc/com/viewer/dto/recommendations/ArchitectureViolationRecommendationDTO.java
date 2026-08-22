package tcc.com.viewer.dto.recommendations;

import java.io.Serializable;
import java.util.List;

public record ArchitectureViolationRecommendationDTO(String classId, String className, String sourceModuleId,
		String sourceModuleName, String violation, List<String> violatingDependencies, MoveSuggestionDTO bestSuggestion,
		List<MoveSuggestionDTO> alternativeSuggestions) implements Serializable {
}
