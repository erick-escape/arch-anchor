package com.archanchor.dto.projects;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.domains.analysis.InvalidAnalysisSettingsException;

/**
 * A project's analysis settings as the API reads and writes them (#36). The values are
 * boxed so that a missing one is reported instead of being read as 0; only
 * {@code maxMoveTargetsPerClass} and the lowest similarity changes of a merge (#41), a
 * move and a split (#73) may be null, meaning no limit; a missing one is read as null, so
 * settings saved before a limit existed keep having none. They are validated by
 * {@link #toAnalysisSettings()}, so the architect gets this project's message rather than
 * a JSON parser's.
 *
 * <pre>
 * {"minSplitModuleClasses": 2, "minSourceClassesAfterMove": 2, "maxMoveTargetsPerClass": 1,
 *  "minClassDependencies": 5, "minRecommendationRate": 0.2, "minMergeSimilarityChange": 0.0,
 *  "minMoveSimilarityChange": 0.0, "minSplitSimilarityChange": null}
 * </pre>
 */
public record AnalysisSettingsDTO(Integer minSplitModuleClasses, Integer minSourceClassesAfterMove,
		Integer maxMoveTargetsPerClass, Integer minClassDependencies, Double minRecommendationRate,
		Double minMergeSimilarityChange, Double minMoveSimilarityChange, Double minSplitSimilarityChange) {

	public static AnalysisSettingsDTO of(AnalysisSettings settings) {
		return new AnalysisSettingsDTO(settings.minSplitModuleClasses(), settings.minSourceClassesAfterMove(),
				settings.maxMoveTargetsPerClass(), settings.minClassDependencies(), settings.minRecommendationRate(),
				settings.minMergeSimilarityChange(), settings.minMoveSimilarityChange(),
				settings.minSplitSimilarityChange());
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
				required("minRecommendationRate", minRecommendationRate), minMergeSimilarityChange,
				minMoveSimilarityChange, minSplitSimilarityChange);
	}

	private static <T> T required(String name, T value) {
		if (value == null) {
			throw new InvalidAnalysisSettingsException("Expected a value for " + name + ", got none");
		}
		return value;
	}

}
