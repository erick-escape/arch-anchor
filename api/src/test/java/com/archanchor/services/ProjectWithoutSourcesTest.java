package com.archanchor.services;

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
 * Regression suite for issue #81. A project without {@code src/} got the modules of the
 * project analysed before it: the list was only cleared once sources were found, and the
 * analysis then elected and saved those modules under the new project's name.
 */
class ProjectWithoutSourcesTest {

	@TempDir
	Path workspace;

	private final ModuleService moduleService = new ModuleService(UniformJavaDependencies.parsersReporting("java.sql"),
			new ActiveRatingWeights());

	@Test
	void aProjectWithoutSourcesHasNoModulesWhateverWasAnalysedBeforeIt() throws IOException {
		Path shop = projectWithFile("shop", "src/main/java/shop/web/CatalogController.java");
		Path notes = projectWithFile("notes", "README.md");

		List<Module> modulesOfShop = moduleService.getModules(shop.toString());
		List<Module> modulesOfNotes = moduleService.getModules(notes.toString());

		assertThat(modulesOfShop).extracting(Module::getName).containsExactly("shop/web");
		assertThat(modulesOfNotes).isEmpty();
	}

	@Test
	void analysingAnotherProjectLeavesTheModulesAlreadyReturnedAlone() throws IOException {
		Path shop = projectWithFile("shop", "src/main/java/shop/web/CatalogController.java");
		Path library = projectWithFile("library", "src/main/java/library/loans/Loan.java");

		List<Module> modulesOfShop = moduleService.getModules(shop.toString());
		moduleService.getModules(library.toString());

		assertThat(modulesOfShop).extracting(Module::getName).containsExactly("shop/web");
	}

	private Path projectWithFile(String projectName, String file) throws IOException {
		Path path = workspace.resolve(projectName).resolve(file);
		Files.createDirectories(path.getParent());
		Files.createFile(path);
		return workspace.resolve(projectName);
	}

}
