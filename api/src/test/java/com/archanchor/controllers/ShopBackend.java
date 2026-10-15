package com.archanchor.controllers;

import com.archanchor.domains.module.Module;
import com.archanchor.mapstruct.ClazzMapperImpl;
import com.archanchor.mapstruct.DependencyMapperImpl;
import com.archanchor.mapstruct.ModuleMapper;
import com.archanchor.mapstruct.ModuleMapperImpl;
import com.archanchor.services.InMemoryModuleService;
import com.archanchor.services.ModuleService;
import com.archanchor.services.ProjectService;
import com.archanchor.services.ProjectsDirectory;
import com.archanchor.services.RecommendationsService;
import com.archanchor.services.analysis.ActiveAnalysisSettings;
import com.archanchor.services.analysis.InMemoryAnalysisSettingsStore;
import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import com.archanchor.services.architecturalAnalyses.ArchitectureViolation;
import com.archanchor.services.architecturalAnalyses.MergeModule;
import com.archanchor.services.architecturalAnalyses.MoveClass;
import com.archanchor.services.architecturalAnalyses.SplitModule;
import com.archanchor.services.parsers.ParserFactory;
import com.archanchor.services.rating.ActiveRatingWeights;
import com.archanchor.services.rating.InMemoryRatingWeightsStore;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * The project, module and recommendation endpoints wired by hand over an
 * {@link InMemoryModuleService}, holding project {@value #PROJECT} the way
 * {@code POST /api/analyze} leaves it: weights adopted, analyses run, modules saved. By
 * default the project is the shop of {@link AnalysedShopFixture}.
 *
 * <pre>
 * ShopBackend backend = ShopBackend.analysed();
 * backend.mockMvc.perform(get("/api/recommendations"));
 * </pre>
 */
final class ShopBackend {

	static final String PROJECT = "shop";

	// Tests of the module and recommendation endpoints never reach the projects
	// directory, so it need not exist.
	private static final Path NO_PROJECTS_DIRECTORY = Path.of("target", "shop-backend-has-no-projects");

	final MockMvc mockMvc;

	final InMemoryRatingWeightsStore ratingWeightsStore = new InMemoryRatingWeightsStore();

	private final ActiveRatingWeights activeWeights = new ActiveRatingWeights();

	final InMemoryAnalysisSettingsStore analysisSettingsStore = new InMemoryAnalysisSettingsStore();

	private final ActiveAnalysisSettings activeSettings = new ActiveAnalysisSettings();

	private final InMemoryModuleService moduleService = new InMemoryModuleService(null, activeWeights);

	private final SplitModule splitModule = new SplitModule(moduleService, activeWeights, activeSettings);

	private final MergeModule mergeModule = new MergeModule(moduleService, activeWeights, activeSettings);

	private final MoveClass moveClass = new MoveClass(moduleService, activeWeights, activeSettings);

	private final ArchitectureViolation architectureViolation = new ArchitectureViolation(moduleService, activeWeights,
			activeSettings);

	// Same order as the running application: Spring registers the analyses by class name.
	private final CountingAnalysesRunner analysesRunner = new CountingAnalysesRunner(
			List.of(architectureViolation, mergeModule, moveClass, splitModule));

	private final ModuleMapper moduleMapper = new ModuleMapperImpl();

	private ShopBackend(Path projectsRoot) {
		ProjectService projectService = new ProjectService(moduleService, new ParserFactory(null), analysesRunner,
				ratingWeightsStore, activeWeights, analysisSettingsStore, activeSettings);
		ModuleController moduleController = new ModuleController(moduleService, new ClazzMapperImpl(),
				new DependencyMapperImpl(), moduleMapper, projectService);
		RecommendationsService recommendationsService = new RecommendationsService(splitModule, mergeModule, moveClass,
				architectureViolation, activeSettings);
		RecommendationsController recommendationsController = new RecommendationsController(recommendationsService,
				moduleController);
		ProjectController projectController = new ProjectController(projectService,
				new ProjectsDirectory(projectsRoot.toString()));
		mockMvc = MockMvcBuilders.standaloneSetup(moduleController, recommendationsController, projectController)
			.build();
	}

	/** How often the analyses have run since {@link #analysed()} set the shop up. */
	int analysisRunsSinceSetUp() {
		return analysesRunner.runs() - 1;
	}

	static ShopBackend analysed() {
		return analysed(AnalysedShopFixture::webAndPersistence, NO_PROJECTS_DIRECTORY);
	}

	/**
	 * Project {@value #PROJECT} built by {@code fixture}, analysed with the weights the
	 * store holds for it, under a projects directory the test owns.
	 */
	static ShopBackend analysed(Function<ModuleService, List<Module>> fixture, Path projectsRoot) {
		ShopBackend backend = new ShopBackend(projectsRoot);
		backend.activeWeights.adopt(PROJECT, backend.ratingWeightsStore.weightsOf(PROJECT));
		backend.activeSettings.adopt(PROJECT, backend.analysisSettingsStore.settingsOf(PROJECT));
		List<Module> modules = fixture.apply(backend.moduleService);
		backend.analysesRunner.executeAll(modules);
		backend.moduleService.saveModules(modules.stream().map(backend.moduleMapper::toDto).toList());
		return backend;
	}

}
