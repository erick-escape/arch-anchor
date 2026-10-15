package com.archanchor.domains.analysis;

import java.io.Serializable;

/**
 * What the analyses recommend and which recommendations are shown, set by the architect
 * per project (#36). On a real system the analyses propose more than anyone can review,
 * much of it from cases the literature leaves out: a one-class module scores similarity 1
 * by definition, every positive move target becomes a card, and a class with one or two
 * dependencies says little about where it belongs. The defaults filter nothing.
 *
 * <pre>
 * AnalysisSettings settings = new AnalysisSettings(2, 2, 1, 5, 0.2);
 * if (settings.allowsSplitHalfOf(half.size())) { ... }
 * </pre>
 *
 * @param minSplitModuleClasses the fewest classes a split may leave in either half
 * @param minSourceClassesAfterMove the fewest classes a move may leave in its source
 * module
 * @param maxMoveTargetsPerClass how many of a class's best-rated move targets are
 * recommended; {@code null} for no limit
 * @param minClassDependencies the fewest package dependencies a class needs before the
 * move and violation analyses judge it; packages, because the similarity is computed on
 * them
 * @param minRecommendationRate the lowest rate of a split, merge or move that is shown
 */
public record AnalysisSettings(int minSplitModuleClasses, int minSourceClassesAfterMove, Integer maxMoveTargetsPerClass,
		int minClassDependencies, double minRecommendationRate) implements Serializable {

	public static final AnalysisSettings DEFAULT = new AnalysisSettings(1, 1, null, 0, 0.0);

	public AnalysisSettings {
		requireAtLeast("minSplitModuleClasses", minSplitModuleClasses, 1);
		requireAtLeast("minSourceClassesAfterMove", minSourceClassesAfterMove, 1);
		if (maxMoveTargetsPerClass != null && maxMoveTargetsPerClass < 1) {
			throw new InvalidAnalysisSettingsException(
					"Expected maxMoveTargetsPerClass to be an integer >= 1, or none for no limit, got "
							+ maxMoveTargetsPerClass);
		}
		requireAtLeast("minClassDependencies", minClassDependencies, 0);
		if (!(minRecommendationRate >= 0.0 && minRecommendationRate <= 1.0)) {
			throw new InvalidAnalysisSettingsException(
					"Expected minRecommendationRate in [0, 1], got " + minRecommendationRate);
		}
	}

	/** Whether a split may recommend a half of {@code classCount} classes. */
	public boolean allowsSplitHalfOf(int classCount) {
		return classCount >= minSplitModuleClasses;
	}

	/** Whether a move may leave its source module with {@code classesLeft} classes. */
	public boolean allowsMoveLeaving(int classesLeft) {
		return classesLeft >= minSourceClassesAfterMove;
	}

	public boolean limitsMoveTargets() {
		return maxMoveTargetsPerClass != null;
	}

	/**
	 * Whether the move and violation analyses judge a class with
	 * {@code packageDependencyCount} package dependencies.
	 */
	public boolean judgesClassWith(int packageDependencyCount) {
		return packageDependencyCount >= minClassDependencies;
	}

	/** Whether a split, merge or move rated {@code rate} is shown. */
	public boolean shows(double rate) {
		return rate >= minRecommendationRate;
	}

	private static void requireAtLeast(String name, int value, int minimum) {
		if (value < minimum) {
			throw new InvalidAnalysisSettingsException(
					"Expected " + name + " to be an integer >= " + minimum + ", got " + value);
		}
	}

}
