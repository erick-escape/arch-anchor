package com.archanchor.services.parsers;

import com.archanchor.domains.dependency.Dependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a Java project is laid out (#81), apart from what its files depend on: which files
 * are its production sources, and that each project is read with nothing remembered from
 * the one before.
 */
class JavaParserTest {

	@TempDir
	Path workspace;

	private final RecordingDependencyReader reader = new RecordingDependencyReader();

	private final JavaParser javaParser = new JavaParser(reader);

	@Test
	void theSourcesAreTheClassFilesOfSrcMainInReadingOrder() throws IOException {
		Path project = projectWithSources("src/main/java/shop/web/OrderController.java",
				"src/main/java/shop/web/package-info.java", "src/main/java/shop/model/Order.java",
				"src/main/resources/application.properties", "src/test/java/shop/model/OrderTest.java");

		assertThat(javaParser.sourceFilesOf(project)).containsExactly(
				project.resolve("src/main/java/shop/model/Order.java"),
				project.resolve("src/main/java/shop/web/OrderController.java"));
	}

	@Test
	void aProjectWithoutSrcHasNoJavaSources() throws IOException {
		Path project = projectWithSources("app/Models/User.php", "lib/Stray.java");

		assertThat(javaParser.sourceFilesOf(project)).isEmpty();
		assertThat(javaParser.clazzesOf(project)).isEmpty();
	}

	@Test
	void aClassIsNamedAfterItsFileAndBelongsToThePackageTheFileDeclares() throws IOException {
		Path project = projectWithSources("src/main/java/shop/web/OrderController.java");
		Files.writeString(project.resolve("src/main/java/shop/web/OrderController.java"),
				"package shop.web;\nclass OrderController {}\n");

		assertThat(javaParser.clazzesOf(project)).singleElement().satisfies(clazz -> {
			assertThat(clazz.name()).isEqualTo("OrderController");
			assertThat(clazz.packageName()).isEqualTo("shop.web");
			assertThat(clazz.sourceFile()).isEqualTo(project.resolve("src/main/java/shop/web/OrderController.java"));
		});
	}

	@Test
	void whatWasResolvedForOneProjectIsForgottenBeforeTheNextIsRead() throws IOException {
		Path project = projectWithSources("src/main/java/shop/web/OrderController.java",
				"src/main/java/shop/model/Order.java");

		javaParser.clazzesOf(project);

		assertThat(reader.calls).containsExactly("clearCache", "Order.java", "OrderController.java");
	}

	private Path projectWithSources(String... sources) throws IOException {
		Path project = workspace.resolve("shop-project");
		for (String source : sources) {
			Path file = project.resolve(source);
			Files.createDirectories(file.getParent());
			Files.createFile(file);
		}
		return project;
	}

	// Resolves nothing; remembers what it was asked, in order.
	private static final class RecordingDependencyReader implements JavaDependencyReader {

		private final List<String> calls = new ArrayList<>();

		@Override
		public List<Dependency> getDependencies(Path javaSource) {
			calls.add(javaSource.getFileName().toString());
			return new ArrayList<>();
		}

		@Override
		public void clearCache() {
			calls.add("clearCache");
		}

	}

}
