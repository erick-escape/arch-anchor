package com.archanchor.services;

import com.archanchor.domains.module.Module;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;
import com.archanchor.services.parsers.ProjectWithoutClassesException;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Regression suite for issue #81. A project without {@code src/} got the modules of the
 * project analysed before it: the list was only cleared once sources were found, and the
 * analysis then elected and saved those modules under the new project's name. Since #87
 * such a project is refused with a reason instead of analysed as empty, and the project
 * analysed before it stays the one in force.
 */
class ProjectWithoutSourcesTest {

	@TempDir
	Path workspace;

	private final ModuleService moduleService = new ModuleService(UniformJavaDependencies.parsersReporting("java.sql"),
			new ActiveRatingWeights());

	@Test
	void aProjectWithoutSourcesIsRefusedWhateverWasAnalysedBeforeIt() throws IOException {
		Path shop = projectWithFile("shop", "src/main/java/shop/web/CatalogController.java");
		Path notes = projectWithFile("notes", "README.md");

		List<Module> modulesOfShop = moduleService.getModules(shop.toString());

		assertThat(modulesOfShop).extracting(Module::getName).containsExactly("shop/web");
		assertThatThrownBy(() -> moduleService.getModules(notes.toString()))
			.isInstanceOf(ProjectWithoutClassesException.class)
			.hasMessageContaining("'notes'")
			.hasMessageContaining("src/main");
	}

	// Its weights and settings must not take the place of the analysed project's either:
	// a change of weights would then re-elect another project's modules.
	@Test
	void aRefusedProjectLeavesTheOneAnalysedBeforeItInForce() throws IOException {
		Path notes = projectWithFile("notes", "README.md");
		HandWiredAnalysis analysis = new HandWiredAnalysis();
		analysis.projectService.analyzeProject("bookstore", Path.of("..", "examples", "bookstore").toString());

		assertThatThrownBy(() -> analysis.projectService.analyzeProject("notes", notes.toString()))
			.isInstanceOf(ProjectWithoutClassesException.class);
		analysis.projectService.updateRatingWeights("bookstore",
				new RatingWeights(WeightPair.EQUAL, new WeightPair(0.9, 0.1)));

		assertThat(analysis.recommendationsService.getRecommendations().splits()).singleElement()
			.satisfies(split -> assertThat(split.metrics().rate()).isCloseTo(0.325, within(1e-4)));
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
