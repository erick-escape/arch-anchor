package com.archanchor.controllers;

import com.archanchor.domains.module.Module;
import com.archanchor.mapstruct.ClazzMapperImpl;
import com.archanchor.mapstruct.DependencyMapperImpl;
import com.archanchor.mapstruct.ModuleMapper;
import com.archanchor.mapstruct.ModuleMapperImpl;
import com.archanchor.services.InMemoryModuleService;
import com.archanchor.services.RecommendationsService;
import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import com.archanchor.services.architecturalAnalyses.ArchitectureViolation;
import com.archanchor.services.architecturalAnalyses.MergeModule;
import com.archanchor.services.architecturalAnalyses.MoveClass;
import com.archanchor.services.architecturalAnalyses.SplitModule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

/**
 * The module and recommendation endpoints wired by hand over an
 * {@link InMemoryModuleService}, holding the shop of {@link AnalysedShopFixture} the way
 * {@code POST /api/analyze} leaves a project: analyses run, modules saved.
 *
 * <pre>
 * ShopBackend backend = ShopBackend.analysed();
 * backend.mockMvc.perform(get("/api/recommendations"));
 * </pre>
 */
final class ShopBackend {

	final MockMvc mockMvc;

	private final InMemoryModuleService moduleService = new InMemoryModuleService();

	private final SplitModule splitModule = new SplitModule(moduleService);

	private final MergeModule mergeModule = new MergeModule(moduleService);

	private final MoveClass moveClass = new MoveClass(moduleService);

	private final ArchitectureViolation architectureViolation = new ArchitectureViolation(moduleService);

	// Same order as the running application: Spring registers the analyses by class name.
	private final ArchitecturalAnalysesRunner analysesRunner = new ArchitecturalAnalysesRunner(
			List.of(architectureViolation, mergeModule, moveClass, splitModule));

	private final ModuleMapper moduleMapper = new ModuleMapperImpl();

	private ShopBackend() {
		ModuleController moduleController = new ModuleController(moduleService, new ClazzMapperImpl(),
				new DependencyMapperImpl(), moduleMapper);
		RecommendationsService recommendationsService = new RecommendationsService(splitModule, mergeModule, moveClass,
				architectureViolation);
		RecommendationsController recommendationsController = new RecommendationsController(recommendationsService,
				moduleController);
		mockMvc = MockMvcBuilders.standaloneSetup(moduleController, recommendationsController).build();
	}

	static ShopBackend analysed() {
		ShopBackend backend = new ShopBackend();
		List<Module> modules = AnalysedShopFixture.webAndPersistence(backend.moduleService);
		backend.analysesRunner.executeAll(modules);
		backend.moduleService.saveModules(modules.stream().map(backend.moduleMapper::toDto).toList());
		return backend;
	}

}
