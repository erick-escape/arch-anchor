package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression suite for issue #47. Every directory under {@code src/} with sources became
 * a module, so a Maven or Gradle project contributed its tests next to its production
 * code: mybatis-3 imported from GitHub had 279 modules instead of 52.
 */
class ProductionSourcesOnlyTest {

	@TempDir
	Path workspace;

	private final ModuleService moduleService = new ModuleService(new UniformDependenciesParserFactory("java.sql"),
			new ActiveRatingWeights());

	@Test
	void theTestsOfAStandardLayoutAreNotModules() throws IOException {
		Path project = projectWithSources("main/java/shop/web/CatalogController.java",
				"test/java/shop/web/CatalogControllerTest.java", "test/java/shop/fixtures/Books.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).singleElement().satisfies(web -> {
			assertThat(web.getName()).isEqualTo("shop/web");
			assertThat(web.getClazzes()).extracting(Clazz::getName).containsExactly("CatalogController");
		});
	}

	// Without src/main, the sources sit directly under src/, as in the projects the tool
	// was first written for.
	@Test
	void aProjectWithoutSrcMainIsAnalysedAsBefore() throws IOException {
		Path project = projectWithSources("shop/web/CatalogController.java", "shop/model/Book.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).extracting(Module::getName).containsExactly("shop/model", "shop/web");
	}

	private Path projectWithSources(String... sources) throws IOException {
		Path project = workspace.resolve("shop-project");
		for (String source : sources) {
			Path file = project.resolve("src").resolve(source);
			Files.createDirectories(file.getParent());
			Files.createFile(file);
		}
		return project;
	}

}
