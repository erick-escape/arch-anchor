package com.archanchor.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * One JSON file holding a value per project, such as the rating weights (#27) or the
 * analysis settings (#36). JSON rather than Java serialization, so the file can be read
 * and corrected by hand. Every call reads the file anew, so a hand edit takes effect
 * without a restart. Not thread-safe: the stores that use it synchronise.
 *
 * <pre>
 * PerProjectJsonFile&lt;RatingWeights&gt; file = new PerProjectJsonFile&lt;&gt;(Path.of("rating-weights.json"),
 * 		RatingWeights.class, "rating weights", "a JSON object from project name to {election, recommendation}");
 * Optional&lt;RatingWeights&gt; weights = file.valueOf("bookstore");
 * </pre>
 */
public class PerProjectJsonFile<T> {

	private final Path file;

	private final String contentName;

	private final String expectedShape;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final JavaType valuesByProject;

	/**
	 * @param contentName what the file holds, for messages, e.g. "rating weights"
	 * @param expectedShape the JSON the file must contain, for the message when it cannot
	 * be read
	 */
	public PerProjectJsonFile(Path file, Class<T> valueType, String contentName, String expectedShape) {
		this.file = file;
		this.contentName = contentName;
		this.expectedShape = expectedShape;
		this.valuesByProject = objectMapper.getTypeFactory().constructMapType(TreeMap.class, String.class, valueType);
	}

	public Optional<T> valueOf(String projectName) {
		return Optional.ofNullable(readAll().get(projectName));
	}

	public void save(String projectName, T value) {
		Map<String, T> values = readAll();
		values.put(projectName, value);
		writeAll(values);
	}

	public void forget(String projectName) {
		Map<String, T> values = readAll();
		if (values.remove(projectName) != null) {
			writeAll(values);
		}
	}

	private Map<String, T> readAll() {
		if (!Files.exists(file)) {
			return new TreeMap<>();
		}
		try {
			return objectMapper.readValue(file.toFile(), valuesByProject);
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not read " + contentName + " from " + file.toAbsolutePath()
					+ "; expected " + expectedShape + ": " + e.getMessage(), e);
		}
	}

	private void writeAll(Map<String, T> values) {
		try {
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), values);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not write " + contentName + " to " + file.toAbsolutePath(), e);
		}
	}

}
