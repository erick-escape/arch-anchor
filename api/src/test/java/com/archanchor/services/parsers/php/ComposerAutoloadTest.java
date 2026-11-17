package com.archanchor.services.parsers.php;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where Composer loads a project's own classes from (#85), read from the {@code autoload}
 * section of {@code composer.json}.
 */
class ComposerAutoloadTest {

	@TempDir
	Path project;

	private final ComposerAutoload composerAutoload = new ComposerAutoload(new ObjectMapper());

	@Test
	void thePsr4DirectoriesAreTheSourceRootsInTheOrderTheyAreListed() throws IOException {
		directories("app", "database/factories", "database/seeders", "tests");
		manifest("""
				{
				  "autoload": {
				    "psr-4": {
				      "App\\\\": "app/",
				      "Database\\\\Factories\\\\": "database/factories/",
				      "Database\\\\Seeders\\\\": "database/seeders/"
				    }
				  },
				  "autoload-dev": { "psr-4": { "Tests\\\\": "tests/" } }
				}
				""");

		assertThat(composerAutoload.sourceRootsOf(project)).containsExactly(project.resolve("app"),
				project.resolve("database/factories"), project.resolve("database/seeders"));
	}

	@Test
	void aPrefixMayNameSeveralDirectories() throws IOException {
		directories("src", "lib");
		manifest("""
				{ "autoload": { "psr-4": { "Acme\\\\": ["src/", "lib/"] } } }
				""");

		assertThat(composerAutoload.sourceRootsOf(project)).containsExactly(project.resolve("src"),
				project.resolve("lib"));
	}

	@Test
	void psr0AndClassmapEntriesAreSourceRootsTooAndAClassmapEntryMayBeAFile() throws IOException {
		directories("library", "legacy");
		Files.createFile(project.resolve("legacy/Mailer.php"));
		manifest("""
				{
				  "autoload": {
				    "psr-0": { "Zend_": "library/" },
				    "classmap": ["legacy/Mailer.php", "legacy"],
				    "files": ["helpers.php"]
				  }
				}
				""");

		assertThat(composerAutoload.sourceRootsOf(project)).containsExactly(project.resolve("library"),
				project.resolve("legacy/Mailer.php"), project.resolve("legacy"));
	}

	@Test
	void aPathNamingTheProjectItselfIsTheProjectDirectory() throws IOException {
		manifest("""
				{ "autoload": { "psr-4": { "Acme\\\\": "", "Other\\\\": "./" } } }
				""");

		assertThat(composerAutoload.sourceRootsOf(project)).containsExactly(project);
	}

	@Test
	void aDirectoryThatDoesNotExistIsNotASourceRoot() throws IOException {
		directories("app");
		manifest("""
				{ "autoload": { "psr-4": { "App\\\\": "app/", "Gone\\\\": "removed/" } } }
				""");

		assertThat(composerAutoload.sourceRootsOf(project)).containsExactly(project.resolve("app"));
	}

	// A manifest is part of what is analysed, not of the tool: it must not make the tool
	// read anything outside the project.
	@Test
	void aPathThatLeavesTheProjectIsNotASourceRoot() throws IOException {
		directories("app");
		Files.createDirectories(project.resolveSibling("shared"));
		manifest(
				"""
						{ "autoload": { "psr-4": { "Up\\\\": "../shared/", "Around\\\\": "app/../../shared", "Root\\\\": "/etc" } } }
						""");

		assertThat(composerAutoload.sourceRootsOf(project)).isEmpty();
	}

	@Test
	void aProjectWithoutAManifestOrWithOneThatAutoloadsNothingHasNoSourceRoots() throws IOException {
		assertThat(composerAutoload.sourceRootsOf(project)).isEmpty();

		manifest("""
				{ "name": "acme/shop", "require": { "php": ">=8.1" } }
				""");

		assertThat(composerAutoload.sourceRootsOf(project)).isEmpty();
	}

	@Test
	void aManifestThatIsNotJsonHasNoSourceRoots() throws IOException {
		manifest("{ \"autoload\": { \"psr-4\": ");

		assertThat(composerAutoload.sourceRootsOf(project)).isEmpty();
	}

	private void manifest(String json) throws IOException {
		Files.writeString(project.resolve("composer.json"), json);
	}

	private void directories(String... paths) throws IOException {
		for (String path : paths) {
			Files.createDirectories(project.resolve(path));
		}
	}

}
