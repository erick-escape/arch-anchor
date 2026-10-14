package com.archanchor.domains.rating;

/**
 * Weights that cannot rate anything: negative, not a number, missing, or not summing to
 * 1. The message is shown to the architect as is, so it names the offending values.
 */
public class InvalidRatingWeightsException extends IllegalArgumentException {

	public InvalidRatingWeightsException(String message) {
		super(message);
	}

}
