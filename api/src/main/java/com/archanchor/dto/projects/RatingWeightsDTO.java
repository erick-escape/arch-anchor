package com.archanchor.dto.projects;

import com.archanchor.domains.rating.InvalidRatingWeightsException;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;

/**
 * A project's rating weights as the API reads and writes them (#27). The weights are
 * boxed so that a missing one is reported instead of being read as 0, and they are only
 * validated by {@link #toRatingWeights()}, so the architect gets this project's message
 * rather than a JSON parser's.
 *
 * <pre>
 * {"election": {"similarity": 0.5, "violation": 0.5}, "recommendation": {"similarity": 0.9, "violation": 0.1}}
 * </pre>
 */
public record RatingWeightsDTO(WeightPairDTO election, WeightPairDTO recommendation) {

	public record WeightPairDTO(Double similarity, Double violation) {
	}

	public static RatingWeightsDTO of(RatingWeights weights) {
		return new RatingWeightsDTO(pairOf(weights.election()), pairOf(weights.recommendation()));
	}

	/**
	 * The validated weights.
	 * @return the weights the DTO describes
	 * @throws InvalidRatingWeightsException naming the pair and the offending values
	 */
	public RatingWeights toRatingWeights() {
		return new RatingWeights(weightPair("election", election), weightPair("recommendation", recommendation));
	}

	private static WeightPairDTO pairOf(WeightPair pair) {
		return new WeightPairDTO(pair.similarity(), pair.violation());
	}

	// A missing pair is left to RatingWeights, which names it.
	private static WeightPair weightPair(String name, WeightPairDTO pair) {
		if (pair == null) {
			return null;
		}
		if (pair.similarity() == null || pair.violation() == null) {
			throw invalid(name, "Expected two weights in [0, 1] summing to 1, got similarity=" + pair.similarity()
					+ ", violation=" + pair.violation());
		}
		try {
			return new WeightPair(pair.similarity(), pair.violation());
		}
		catch (InvalidRatingWeightsException e) {
			throw invalid(name, e.getMessage());
		}
	}

	private static InvalidRatingWeightsException invalid(String name, String reason) {
		return new InvalidRatingWeightsException("Invalid " + name + " weights. " + reason);
	}

}
