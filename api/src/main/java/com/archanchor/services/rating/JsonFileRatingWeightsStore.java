package com.archanchor.services.rating;

import com.archanchor.domains.rating.RatingWeights;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

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

	private static final TypeReference<TreeMap<String, RatingWeights>> WEIGHTS_BY_PROJECT = new TypeReference<>() {
	};

	private final Path file;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public JsonFileRatingWeightsStore(@Value("${archanchor.rating-weights-file:rating-weights.json}") String file) {
		this.file = Path.of(file);
	}

	@Override
	public synchronized RatingWeights weightsOf(String projectName) {
		return readAll().getOrDefault(projectName, RatingWeights.EQUAL);
	}

	@Override
	public synchronized void save(String projectName, RatingWeights weights) {
		Map<String, RatingWeights> weightsByProject = readAll();
		weightsByProject.put(projectName, weights);
		writeAll(weightsByProject);
	}

	@Override
	public synchronized void forget(String projectName) {
		Map<String, RatingWeights> weightsByProject = readAll();
		if (weightsByProject.remove(projectName) != null) {
			writeAll(weightsByProject);
		}
	}

	private Map<String, RatingWeights> readAll() {
		if (!Files.exists(file)) {
			return new TreeMap<>();
		}
		try {
			return objectMapper.readValue(file.toFile(), WEIGHTS_BY_PROJECT);
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not read rating weights from " + file.toAbsolutePath()
					+ "; expected a JSON object from project name to {election, recommendation}, each "
					+ "{similarity, violation}: " + e.getMessage(), e);
		}
	}

	private void writeAll(Map<String, RatingWeights> weightsByProject) {
		try {
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), weightsByProject);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not write rating weights to " + file.toAbsolutePath(), e);
		}
	}

}
