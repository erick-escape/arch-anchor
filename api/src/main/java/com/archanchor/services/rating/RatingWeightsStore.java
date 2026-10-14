package com.archanchor.services.rating;

import com.archanchor.domains.rating.RatingWeights;

/**
 * Where each project's rating weights are kept between analyses and restarts (#27).
 *
 * <pre>
 * ratingWeightsStore.save("bookstore", new RatingWeights(WeightPair.EQUAL, new WeightPair(0.9, 0.1)));
 * RatingWeights weights = ratingWeightsStore.weightsOf("bookstore");
 * </pre>
 */
public interface RatingWeightsStore {

	/** The project's weights, or {@link RatingWeights#EQUAL} if none were set. */
	RatingWeights weightsOf(String projectName);

	void save(String projectName, RatingWeights weights);

	/**
	 * Back to equal weights, so a new project of the same name starts from the default.
	 */
	void forget(String projectName);

}
