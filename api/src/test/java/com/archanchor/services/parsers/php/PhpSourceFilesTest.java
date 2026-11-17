package com.archanchor.services.parsers.php;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which files are a PHP project's production sources (#85): what Composer autoloads or,
 * failing that, every PHP file that is not third-party code.
 */
class PhpSourceFilesTest {

	@TempDir
	Path workspace;

	private final PhpSourceFiles sourceFiles = new PhpSourceFiles(new ComposerAutoload(new ObjectMapper()));

	@Test
	void theSourcesAreThePhpFilesOfTheAutoloadedDirectoriesInReadingOrder() throws IOException {
		Path project = projectWithFiles("composer.json", "app/Models/Order.php", "app/Models/Book.php",
				"app/Http/Kernel.php", "app/README.md", "database/seeders/BookSeeder.php",
				"database/migrations/2024_01_01_create_books.php", "resources/views/home.blade.php", "routes/web.php",
				"tests/OrderTest.php", "vendor/autoload.php", "vendor/acme/lib/Thing.php");
		Files.writeString(project.resolve("composer.json"), """
				{
				  "autoload": { "psr-4": { "App\\\\": "app/", "Database\\\\Seeders\\\\": "database/seeders/" } },
				  "autoload-dev": { "psr-4": { "Tests\\\\": "tests/" } }
				}
				""");

		assertThat(relative(project, sourceFiles.in(project))).containsExactly("app/Http/Kernel.php",
				"app/Models/Book.php", "app/Models/Order.php", "database/seeders/BookSeeder.php");
	}

	@Test
	void withoutAnAutoloadSectionEveryPhpFileOfTheProjectIsASource() throws IOException {
		Path project = projectWithFiles("index.php", "includes/db.php", "includes/Mailer.PHP", "assets/app.js",
				"lib/shapes/Square.php");

		assertThat(relative(project, sourceFiles.in(project))).containsExactly("index.php", "includes/Mailer.PHP",
				"includes/db.php", "lib/shapes/Square.php");
	}

	@Test
	void thirdPartyCodeIsNeverASource() throws IOException {
		Path project = projectWithFiles("index.php", "vendor/autoload.php", "vendor/acme/lib/Thing.php",
				"node_modules/some-package/build.php", ".git/hooks/pre-commit.php", "lib/.cache/Compiled.php");

		assertThat(relative(project, sourceFiles.in(project))).containsExactly("index.php");
	}

	// Composer writes autoload.php into the directory it installs packages in; a
	// directory of the project's own that is merely called vendor has none.
	@Test
	void aDirectoryCalledVendorThatComposerDidNotWriteIsTheProjectsOwn() throws IOException {
		Path project = projectWithFiles("app/Vendor/Supplier.php", "app/vendor/Catalogue.php");

		assertThat(relative(project, sourceFiles.in(project))).containsExactly("app/Vendor/Supplier.php",
				"app/vendor/Catalogue.php");
	}

	// "Acme\\": "" autoloads from the project root, which also holds what Composer
	// installed.
	@Test
	void anAutoloadedDirectoryThatHoldsThirdPartyCodeDoesNotMakeItASource() throws IOException {
		Path project = projectWithFiles("composer.json", "Shop.php", "vendor/autoload.php",
				"vendor/acme/lib/Thing.php");
		Files.writeString(project.resolve("composer.json"), """
				{ "autoload": { "psr-4": { "Acme\\\\": "" } } }
				""");

		assertThat(relative(project, sourceFiles.in(project))).containsExactly("Shop.php");
	}

	@Test
	void aFileListedTwiceIsOneSource() throws IOException {
		Path project = projectWithFiles("composer.json", "src/Shop.php", "src/legacy/Mailer.php");
		Files.writeString(project.resolve("composer.json"), """
				{ "autoload": { "psr-4": { "Acme\\\\": "src/" }, "classmap": ["src/legacy", "src/legacy/Mailer.php"] } }
				""");

		assertThat(relative(project, sourceFiles.in(project))).containsExactly("src/Shop.php", "src/legacy/Mailer.php");
	}

	@Test
	void aProjectThatHoldsNoPhpOrDoesNotExistHasNoSources() throws IOException {
		Path javaProject = projectWithFiles("src/main/java/shop/Catalog.java", "pom.xml");

		assertThat(sourceFiles.in(javaProject)).isEmpty();
		assertThat(sourceFiles.in(workspace.resolve("missing"))).isEmpty();
	}

	private Path projectWithFiles(String... files) throws IOException {
		Path project = workspace.resolve("shop");
		for (String file : files) {
			Path path = project.resolve(file);
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		}
		return project;
	}

	private static List<String> relative(Path project, List<Path> files) {
		return files.stream().map(file -> project.relativize(file).toString().replace('\\', '/')).toList();
	}

}
