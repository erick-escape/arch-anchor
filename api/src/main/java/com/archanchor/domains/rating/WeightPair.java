package com.archanchor.domains.rating;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * How much similarity and violations each count when something is rated. The two weights
 * sum to 1, so a rate stays on the scale the priority thresholds of the recommendations
 * were set for (#27).
 *
 * <pre>
 * double rate = new WeightPair(0.8, 0.2).rate(similarityImprovement, violationsImprovement);
 * </pre>
 */
public record WeightPair(double similarity, double violation) implements Serializable {

	/** The balance of the ECSA 2026 rating mechanism, and the default. */
	public static final WeightPair EQUAL = new WeightPair(0.5, 0.5);

	// Slider values such as 0.7 and 0.3 do not sum to exactly 1 in binary floating point.
	private static final double SUM_TOLERANCE = 1e-9;

	public WeightPair {
		if (!isWeight(similarity) || !isWeight(violation)) {
			throw invalid(similarity, violation, "");
		}
		if (Math.abs(similarity + violation - 1.0) > SUM_TOLERANCE) {
			throw invalid(similarity, violation, " (sum " + decimalSum(similarity, violation) + ")");
		}
	}

	/**
	 * Combines a similarity term and a violation term, each in [0, 1] or a change of one.
	 */
	public double rate(double similarityTerm, double violationTerm) {
		return similarity * similarityTerm + violation * violationTerm;
	}

	private static boolean isWeight(double weight) {
		return weight >= 0.0 && weight <= 1.0;
	}

	// The sum as the architect would write it: 0.3 + 0.6 is 0.9, not 0.8999999999999999.
	private static String decimalSum(double similarity, double violation) {
		return BigDecimal.valueOf(similarity).add(BigDecimal.valueOf(violation)).toPlainString();
	}

	private static InvalidRatingWeightsException invalid(double similarity, double violation, String detail) {
		return new InvalidRatingWeightsException("Expected two weights in [0, 1] summing to 1, got similarity="
				+ similarity + ", violation=" + violation + detail);
	}

}
