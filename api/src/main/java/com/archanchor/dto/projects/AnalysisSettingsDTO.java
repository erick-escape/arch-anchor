package com.archanchor.dto.projects;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.domains.analysis.InvalidAnalysisSettingsException;

/**
 * A project's analysis settings as the API reads and writes them (#36). The values are
 * boxed so that a missing one is reported instead of being read as 0; only
 * {@code maxMoveTargetsPerClass} may be null, meaning no limit. They are validated by
 * {@link #toAnalysisSettings()}, so the architect gets this project's message rather than
 * a JSON parser's.
 *
 * <pre>
 * {"minSplitModuleClasses": 2, "minSourceClassesAfterMove": 2, "maxMoveTargetsPerClass": 1,
 *  "minClassDependencies": 5, "minRecommendationRate": 0.2}
 * </pre>
 */
public record AnalysisSettingsDTO(Integer minSplitModuleClasses, Integer minSourceClassesAfterMove,
		Integer maxMoveTargetsPerClass, Integer minClassDependencies, Double minRecommendationRate) {

	public static AnalysisSettingsDTO of(AnalysisSettings settings) {
		return new AnalysisSettingsDTO(settings.minSplitModuleClasses(), settings.minSourceClassesAfterMove(),
				settings.maxMoveTargetsPerClass(), settings.minClassDependencies(), settings.minRecommendationRate());
	}

	/**
	 * The validated settings.
	 * @return the settings the DTO describes
	 * @throws InvalidAnalysisSettingsException naming the missing or offending setting
	 */
	public AnalysisSettings toAnalysisSettings() {
		return new AnalysisSettings(required("minSplitModuleClasses", minSplitModuleClasses),
				required("minSourceClassesAfterMove", minSourceClassesAfterMove), maxMoveTargetsPerClass,
				required("minClassDependencies", minClassDependencies),
				required("minRecommendationRate", minRecommendationRate));
	}

	private static <T> T required(String name, T value) {
		if (value == null) {
			throw new InvalidAnalysisSettingsException("Expected a value for " + name + ", got none");
		}
		return value;
	}

}
