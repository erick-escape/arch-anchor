package com.archanchor.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Everything the analysis returns for {@code examples/bookstore}, down to the order of
 * each module's classes and the types behind each dependency (#81).
 * {@link BookstoreRunningExampleTest} pins the numbers the README quotes; this pins the
 * rest, so that a change in how sources are read cannot alter what a Java project gets
 * without a test saying so. Numbers are compared at full precision, which also catches a
 * change in the order they are summed in.
 *
 * <p>
 * When a change to the result is intended, the test leaves what it got in
 * {@code target/golden/bookstore-analysis.json}: compare it with the expected file in
 * {@code src/test/resources/golden/} and copy it over.
 */
class BookstoreWholeAnalysisTest {

	private static final String BOOKSTORE = Path.of("..", "examples", "bookstore").toString();

	private static final String EXPECTED_ANALYSIS = "/golden/bookstore-analysis.json";

	private static final Path ANALYSIS_AS_RETURNED = Path.of("target", "golden", "bookstore-analysis.json");

	// A class's id is random, and a module's is derived from the path of its directory as
	// the machine running the test writes it.
	private static final Set<String> ID_FIELDS = Set.of("id", "moduleId", "refClassId");

	private final ObjectMapper json = new ObjectMapper();

	private final HandWiredAnalysis analysis = new HandWiredAnalysis();

	@Test
	void theBookstoreIsAnalysedAsRecorded() throws IOException {
		String returned = printed(
				withoutIds(json.valueToTree(analysis.projectService.analyzeProject("bookstore", BOOKSTORE))));
		keepForComparison(returned);

		assertThat(returned)
			.as("the analysis of examples/bookstore, kept in %s, against src/test/resources%s", ANALYSIS_AS_RETURNED,
					EXPECTED_ANALYSIS)
			.isEqualTo(printed(expectedAnalysis()));
	}

	private JsonNode expectedAnalysis() throws IOException {
		try (InputStream expected = getClass().getResourceAsStream(EXPECTED_ANALYSIS)) {
			assertThat(expected).as("the recorded analysis, src/test/resources%s", EXPECTED_ANALYSIS).isNotNull();
			return json.readTree(expected);
		}
	}

	private static JsonNode withoutIds(JsonNode node) {
		if (node instanceof ObjectNode object) {
			object.remove(ID_FIELDS);
		}
		node.forEach(BookstoreWholeAnalysisTest::withoutIds);
		return node;
	}

	// Printed the same way on both sides, so the comparison does not depend on how the
	// expected file was indented or which line endings a checkout gave it.
	private String printed(JsonNode node) throws IOException {
		return json.writerWithDefaultPrettyPrinter().writeValueAsString(node).replace("\r\n", "\n");
	}

	private static void keepForComparison(String returned) throws IOException {
		Files.createDirectories(ANALYSIS_AS_RETURNED.getParent());
		Files.writeString(ANALYSIS_AS_RETURNED, returned + "\n");
	}

}
