package com.archanchor.services;

import com.archanchor.util.FourDecimalPlaces;

/**
 * The priority of a split, merge or move: above 0.5 high, above 0.2 medium, low
 * otherwise. The web derives the same badge from the rate the API writes
 * (utils/priority.ts), so the rate is classified rounded as the API writes it (#71).
 *
 * <pre>
 * RecommendationPriority.of(0.5625); // HIGH
 * </pre>
 */
public enum RecommendationPriority {

	HIGH, MEDIUM, LOW;

	private static final double HIGH_PRIORITY_THRESHOLD = 0.5;

	private static final double MEDIUM_PRIORITY_THRESHOLD = 0.2;

	public static RecommendationPriority of(double rate) {
		double written = FourDecimalPlaces.round(rate);
		if (written > HIGH_PRIORITY_THRESHOLD) {
			return HIGH;
		}
		return written > MEDIUM_PRIORITY_THRESHOLD ? MEDIUM : LOW;
	}

}
