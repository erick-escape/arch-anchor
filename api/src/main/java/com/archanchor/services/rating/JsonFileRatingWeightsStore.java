package com.archanchor.services.rating;

import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.util.PerProjectJsonFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Keeps every project's rating weights in one JSON file, by default
 * {@code rating-weights.json} beside {@code modules.bin}. JSON rather than Java
 * serialization, so the file can be read and corrected by hand.
 *
 * <pre>
 * RatingWeightsStore store = new JsonFileRatingWeightsStore("rating-weights.json");
 * store.save("bookstore", RatingWeights.EQUAL);
 * </pre>
 */
@Component
public class JsonFileRatingWeightsStore implements RatingWeightsStore {

	private final PerProjectJsonFile<RatingWeights> file;

	public JsonFileRatingWeightsStore(@Value("${archanchor.rating-weights-file:rating-weights.json}") String file) {
		this.file = new PerProjectJsonFile<>(Path.of(file), RatingWeights.class, "rating weights",
				"a JSON object from project name to {election, recommendation}, each {similarity, violation}");
	}

	@Override
	public synchronized RatingWeights weightsOf(String projectName) {
		return file.valueOf(projectName).orElse(RatingWeights.EQUAL);
	}

	@Override
	public synchronized void save(String projectName, RatingWeights weights) {
		file.save(projectName, weights);
	}

	@Override
	public synchronized void forget(String projectName) {
		file.forget(projectName);
	}

}
