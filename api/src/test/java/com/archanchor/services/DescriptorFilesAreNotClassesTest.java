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
 * Regression suite for issue #31. {@code package-info.java} and {@code module-info.java}
 * declare a package and a Java module, not a class, yet each became a class without
 * dependencies: it lowered its module's similarity and was always the far end of a split.
 */
class DescriptorFilesAreNotClassesTest {

	@TempDir
	Path workspace;

	private final ModuleService moduleService = new ModuleService(new UniformDependenciesParserFactory("java.sql"),
			new ActiveRatingWeights());

	@Test
	void packageInfoIsNotOneOfItsModulesClasses() throws IOException {
		Path project = projectWithSources("shop/web/CatalogController.java", "shop/web/package-info.java",
				"shop/web/OrderController.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).singleElement()
			.satisfies(web -> assertThat(web.getClazzes()).extracting(Clazz::getName)
				.containsExactly("CatalogController", "OrderController"));
	}

	@Test
	void moduleInfoDoesNotMakeTheSourceRootAModule() throws IOException {
		Path project = projectWithSources("module-info.java", "shop/web/CatalogController.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).extracting(Module::getName).containsExactly("shop/web");
	}

	@Test
	void aDirectoryWithOnlyAPackageInfoIsNotAModule() throws IOException {
		Path project = projectWithSources("shop/package-info.java", "shop/web/CatalogController.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).extracting(Module::getName).containsExactly("shop/web");
	}

	private Path projectWithSources(String... sources) throws IOException {
		Path project = workspace.resolve("shop-project");
		for (String source : sources) {
			Path file = project.resolve("src/main/java").resolve(source);
			Files.createDirectories(file.getParent());
			Files.createFile(file);
		}
		return project;
	}

}
