package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression suite for issue #15. Module and class order used to be whatever the
 * filesystem returned, and a tie between equally rated classes goes to the first one, so
 * the same sources could elect different reference classes depending on where they sat on
 * disk. Each test writes its files in an order that is neither ascending nor descending,
 * so that no filesystem lists them sorted by accident of creation order.
 */
class ModuleDiscoveryOrderTest {

	@TempDir
	Path workspace;

	private final ModuleService moduleService = new ModuleService(new UniformDependenciesParserFactory("java.sql"));

	@Test
	void classesAreOrderedByFileNameNotByCreationOrder() throws IOException {
		Path project = projectWithSources("shop", "shop/web/StockController.java", "shop/web/CatalogController.java",
				"shop/web/OrderController.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).singleElement()
			.satisfies(web -> assertThat(web.getClazzes()).extracting(Clazz::getName)
				.containsExactly("CatalogController", "OrderController", "StockController"));
	}

	@Test
	void modulesAreOrderedByPathNotByCreationOrder() throws IOException {
		Path project = projectWithSources("shop", "shop/web/OrderController.java", "shop/model/Order.java",
				"shop/service/OrderService.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).extracting(Module::getName).containsExactly("shop/model", "shop/service", "shop/web");
	}

	@Test
	void fileNamesAreComparedAsPlainStringsSoTheOrderIsTheSameOnEveryPlatform() throws IOException {
		// A case-insensitive filesystem comparison would give Uri, UrlMapper, URLParser.
		Path project = projectWithSources("shop", "shop/web/UrlMapper.java", "shop/web/URLParser.java",
				"shop/web/Uri.java");

		List<Module> modules = moduleService.getModules(project.toString());

		assertThat(modules).singleElement()
			.satisfies(web -> assertThat(web.getClazzes()).extracting(Clazz::getName)
				.containsExactly("URLParser", "Uri", "UrlMapper"));
	}

	@Test
	void theSameSourcesElectTheSameReferenceClassWhateverOrderTheyWereWrittenIn() throws IOException {
		Path writtenOneWay = projectWithSources("one-way", "shop/web/StockController.java",
				"shop/web/CatalogController.java", "shop/web/OrderController.java");
		Path writtenAnotherWay = projectWithSources("another-way", "shop/web/OrderController.java",
				"shop/web/StockController.java", "shop/web/CatalogController.java");

		// All three classes have the same dependencies, so the election is a three-way
		// tie.
		assertThat(referenceClassOf(writtenOneWay)).isEqualTo("CatalogController");
		assertThat(referenceClassOf(writtenAnotherWay)).isEqualTo("CatalogController");
	}

	private String referenceClassOf(Path project) throws IOException {
		Module onlyModule = moduleService.getModules(project.toString()).get(0);
		moduleService.populateModuleDependencies(onlyModule);
		moduleService.calculateClassSimilaritiesAndSelectRefClasses(onlyModule);
		return onlyModule.getRefClazzes().get(0).getName();
	}

	private Path projectWithSources(String projectName, String... sourcesInCreationOrder) throws IOException {
		Path project = workspace.resolve(projectName);
		for (String source : sourcesInCreationOrder) {
			Path file = project.resolve("src/main/java").resolve(source);
			Files.createDirectories(file.getParent());
			Files.createFile(file);
		}
		return project;
	}

}
