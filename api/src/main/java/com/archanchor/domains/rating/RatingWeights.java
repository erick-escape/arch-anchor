package com.archanchor.domains.rating;

import java.io.Serializable;

/**
 * The two weightings an architect sets for a project. They are kept apart because they
 * answer different questions: which class represents a module ({@code election}), and
 * which refactoring is worth making ({@code recommendation}) (#27).
 *
 * <pre>
 * RatingWeights weights = new RatingWeights(WeightPair.EQUAL, new WeightPair(0.9, 0.1));
 * </pre>
 */
public record RatingWeights(WeightPair election, WeightPair recommendation) implements Serializable {

	public static final RatingWeights EQUAL = new RatingWeights(WeightPair.EQUAL, WeightPair.EQUAL);

	public RatingWeights {
		if (election == null) {
			throw new InvalidRatingWeightsException("Expected an election pair {similarity, violation}, got none");
		}
		if (recommendation == null) {
			throw new InvalidRatingWeightsException("Expected a recommendation pair {similarity, violation}, got none");
		}
	}

}
