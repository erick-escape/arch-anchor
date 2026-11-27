package com.archanchor.services.repositoryImport;

import com.archanchor.services.parsers.EveryLanguage;
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

/**
 * Which directory of a checkout becomes the project: one in which a parser finds sources,
 * whatever the layout of its language (#87).
 */
class AnalysisRootLocatorTest {

	@TempDir
	Path workspace;

	private Path checkout;

	private final AnalysisRootLocator locator = new AnalysisRootLocator(EveryLanguage.parsers());

	@BeforeEach
	void createCheckout() throws IOException {
		checkout = Files.createDirectories(workspace.resolve("checkout"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "." })
	void usesTheCheckoutWhenNoSubdirectoryIsNamed(String subdirectory) throws IOException {
		source("src/main/java/shop/Order.java");

		assertThat(locator.locate(checkout, subdirectory)).isEqualTo(checkout);
	}

	@ParameterizedTest
	@ValueSource(strings = { "examples/demo", "examples/demo/", "./examples/demo", " examples/demo " })
	void usesTheSubdirectoryTheUserNamed(String subdirectory) throws IOException {
		source("examples/demo/src/main/java/demo/Demo.java");

		assertThat(locator.locate(checkout, subdirectory)).isEqualTo(checkout.resolve("examples/demo"));
	}

	// A Laravel application keeps its classes in app/ and has no src/ at all.
	@Test
	void acceptsAPhpProjectWhereverItsComposerManifestAutoloadsFrom() throws IOException {
		source("app/Models/Order.php");
		Files.writeString(checkout.resolve("composer.json"), """
				{ "autoload": { "psr-4": { "App\\\\": "app/" } } }
				""");

		assertThat(locator.locate(checkout, null)).isEqualTo(checkout);
	}

	@Test
	void acceptsAPhpProjectWithoutAManifest() throws IOException {
		source("legacy/includes/Mailer.php");

		assertThat(locator.locate(checkout, "legacy")).isEqualTo(checkout.resolve("legacy"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "..", "../outside", "/etc", "examples/../../outside" })
	void refusesSubdirectoriesOutsideTheCheckout(String subdirectory) throws IOException {
		Files.createDirectories(workspace.resolve("outside/src"));

		assertThatThrownBy(() -> locator.locate(checkout, subdirectory))
			.isInstanceOf(InvalidSubdirectoryException.class)
			.hasMessageContaining("'" + subdirectory + "'");
	}

	// An empty src/ was enough before #87, and the analysis then found nothing.
	@Test
	void refusesARootWhoseSourceDirectoryHoldsNoSources() throws IOException {
		Files.createDirectories(checkout.resolve("src/main/java"));

		assertThatThrownBy(() -> locator.locate(checkout, null)).isInstanceOf(MissingSourceDirectoryException.class);
	}

	@Test
	void saysWhatEachLanguageExpectsWhenTheRootHoldsNoSources() throws IOException {
		Files.createDirectories(checkout.resolve("docs"));

		assertThatThrownBy(() -> locator.locate(checkout, null)).isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("Found no sources to analyse in the repository root")
			.hasMessageContaining("Java sources under src/main/")
			.hasMessageContaining("PHP sources in the directories composer.json autoloads")
			.hasMessageContaining("No directory within 4 levels of the root holds any.");
	}

	@Test
	void listsTheShallowestDirectoriesThatHoldSources() throws IOException {
		source("api/src/main/java/shop/Order.java");
		source("examples/demo/src/main/java/demo/Demo.java");
		Files.createDirectories(checkout.resolve("docs"));

		assertThatThrownBy(() -> locator.locate(checkout, null)).isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("repository root")
			.hasMessageEndingWith("Set the subdirectory to one of: api, examples/demo.");
	}

	@Test
	void listsDirectoriesOfEitherLanguageWhenTheNamedOneHoldsNoSources() throws IOException {
		source("api/src/main/java/shop/Order.java");
		source("examples/demo/src/main/java/demo/Demo.java");
		source("site/app/Http/Kernel.php");
		Files.createDirectories(checkout.resolve("docs"));

		assertThatThrownBy(() -> locator.locate(checkout, "docs")).isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("'docs'")
			.hasMessageEndingWith("Set the subdirectory to one of: api, site, examples/demo.");
	}

	// Without a composer.json every PHP file of a directory is a source (#85), so a root
	// with PHP anywhere below it is a PHP project. If those files declare no class, the
	// analysis says so (#87), and the subdirectory can be set then.
	@Test
	void aRootWithPhpAnywhereBelowItIsAPhpProject() throws IOException {
		source("api/src/main/java/shop/Order.java");
		source("tools/release.php");

		assertThat(locator.locate(checkout, null)).isEqualTo(checkout);
	}

	@Test
	void listsTheCandidatesWhenTheNamedSubdirectoryDoesNotExist() throws IOException {
		source("backend/src/main/java/shop/Order.java");

		assertThatThrownBy(() -> locator.locate(checkout, "bakend")).isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("'bakend'")
			.hasMessageContaining("one of: backend.");
	}

	@Test
	void neverSuggestsADirectoryWhoseNameStartsWithADot() throws IOException {
		source(".github/scripts/src/main/java/ci/Check.java");

		assertThatThrownBy(() -> locator.locate(checkout, null)).hasMessageContaining("No directory");
	}

	private void source(String path) throws IOException {
		Path file = checkout.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, path.endsWith(".php") ? "<?php class A {}" : "class A {}");
	}

}
