package com.archanchor.services.rating;

import com.archanchor.domains.rating.RatingWeights;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link RatingWeightsStore} without the {@code rating-weights.json} file, so tests leave
 * the settings of whoever runs them alone.
 *
 * <pre>
 * InMemoryRatingWeightsStore store = new InMemoryRatingWeightsStore();
 * store.save("shop", new RatingWeights(new WeightPair(0.9, 0.1), WeightPair.EQUAL));
 * </pre>
 */
public class InMemoryRatingWeightsStore implements RatingWeightsStore {

	private final Map<String, RatingWeights> weightsByProject = new HashMap<>();

	@Override
	public RatingWeights weightsOf(String projectName) {
		return weightsByProject.getOrDefault(projectName, RatingWeights.EQUAL);
	}

	@Override
	public void save(String projectName, RatingWeights weights) {
		weightsByProject.put(projectName, weights);
	}

	@Override
	public void forget(String projectName) {
		weightsByProject.remove(projectName);
	}

}
