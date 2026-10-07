package com.archanchor.services.repositoryImport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisRootLocatorTest {

	@TempDir
	Path workspace;

	private Path checkout;

	private final AnalysisRootLocator locator = new AnalysisRootLocator();

	@BeforeEach
	void createCheckout() throws IOException {
		checkout = Files.createDirectories(workspace.resolve("checkout"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "." })
	void usesTheCheckoutWhenNoSubdirectoryIsNamed(String subdirectory) throws IOException {
		Files.createDirectories(checkout.resolve("src"));

		assertThat(locator.locate(checkout, subdirectory)).isEqualTo(checkout);
	}

	@ParameterizedTest
	@ValueSource(strings = { "examples/demo", "examples/demo/", "./examples/demo", " examples/demo " })
	void usesTheSubdirectoryTheUserNamed(String subdirectory) throws IOException {
		Files.createDirectories(checkout.resolve("examples/demo/src"));

		assertThat(locator.locate(checkout, subdirectory)).isEqualTo(checkout.resolve("examples/demo"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "..", "../outside", "/etc", "examples/../../outside" })
	void refusesSubdirectoriesOutsideTheCheckout(String subdirectory) throws IOException {
		Files.createDirectories(workspace.resolve("outside/src"));

		assertThatThrownBy(() -> locator.locate(checkout, subdirectory))
			.isInstanceOf(InvalidSubdirectoryException.class)
			.hasMessageContaining("'" + subdirectory + "'");
	}

	@Test
	void listsTheDirectoriesThatHoldSrcWhenTheRootDoesNot() throws IOException {
		Files.createDirectories(checkout.resolve("api/src"));
		Files.createDirectories(checkout.resolve("examples/demo/src"));
		Files.createDirectories(checkout.resolve("docs"));

		assertThatThrownBy(() -> locator.locate(checkout, null)).isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("repository root")
			.hasMessageContaining("api, examples/demo");
	}

	@Test
	void listsTheCandidatesWhenTheNamedSubdirectoryDoesNotExist() throws IOException {
		Files.createDirectories(checkout.resolve("backend/src"));

		assertThatThrownBy(() -> locator.locate(checkout, "bakend")).isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("'bakend'")
			.hasMessageContaining("backend");
	}

	@Test
	void saysSoWhenNoDirectoryHoldsSrc() throws IOException {
		Files.createDirectories(checkout.resolve("docs"));

		assertThatThrownBy(() -> locator.locate(checkout, null)).isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("No directory");
	}

}
