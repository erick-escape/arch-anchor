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
 * AnalysisSettings settings = new AnalysisSettings(2, 2, 1, 5, 0.2, 0.0, 0.0, null);
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
 * @param minMergeSimilarityChange the lowest similarity change of a recommended merge, so
 * that 0 keeps out merges that only remove violations at a cost in similarity (#41);
 * {@code null} for no limit
 * @param minMoveSimilarityChange the lowest similarity change of a recommended move,
 * applied before each class's best targets are kept (#73); {@code null} for no limit
 * @param minSplitSimilarityChange the lowest similarity change of a recommended split
 * (#73); {@code null} for no limit
 */
public record AnalysisSettings(int minSplitModuleClasses, int minSourceClassesAfterMove, Integer maxMoveTargetsPerClass,
		int minClassDependencies, double minRecommendationRate, Double minMergeSimilarityChange,
		Double minMoveSimilarityChange, Double minSplitSimilarityChange) implements Serializable {

	public static final AnalysisSettings DEFAULT = new AnalysisSettings(1, 1, null, 0, 0.0, null, null, null);

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
		requireSimilarityChangeLimit("minMergeSimilarityChange", minMergeSimilarityChange);
		requireSimilarityChangeLimit("minMoveSimilarityChange", minMoveSimilarityChange);
		requireSimilarityChangeLimit("minSplitSimilarityChange", minSplitSimilarityChange);
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

	/**
	 * Whether a merge that changes similarity by {@code similarityChange} (merged minus
	 * the mean of the two modules') may be recommended (#41).
	 */
	public boolean allowsMergeChangingSimilarityBy(double similarityChange) {
		return isWithinLimit(similarityChange, minMergeSimilarityChange);
	}

	/**
	 * Whether a move that changes similarity by {@code similarityChange} (the mean of its
	 * source's and target's, after minus before) may be recommended (#73).
	 */
	public boolean allowsMoveChangingSimilarityBy(double similarityChange) {
		return isWithinLimit(similarityChange, minMoveSimilarityChange);
	}

	/**
	 * Whether a split that changes similarity by {@code similarityChange} (the mean of
	 * the halves' minus the module's) may be recommended (#73).
	 */
	public boolean allowsSplitChangingSimilarityBy(double similarityChange) {
		return isWithinLimit(similarityChange, minSplitSimilarityChange);
	}

	/** Whether a split, merge or move rated {@code rate} is shown. */
	public boolean shows(double rate) {
		return rate >= minRecommendationRate;
	}

	private static boolean isWithinLimit(double similarityChange, Double minimum) {
		return minimum == null || similarityChange >= minimum;
	}

	// A similarity change lies in [-1, 1], so no other limit can matter
	private static void requireSimilarityChangeLimit(String name, Double minimum) {
		if (minimum != null && !(minimum >= -1.0 && minimum <= 1.0)) {
			throw new InvalidAnalysisSettingsException(
					"Expected " + name + " in [-1, 1], or none for no limit, got " + minimum);
		}
	}

	private static void requireAtLeast(String name, int value, int minimum) {
		if (value < minimum) {
			throw new InvalidAnalysisSettingsException(
					"Expected " + name + " to be an integer >= " + minimum + ", got " + value);
		}
	}

}
