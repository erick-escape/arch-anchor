package com.archanchor.services;

import com.archanchor.services.analysis.ActiveAnalysisSettings;
import com.archanchor.services.analysis.InMemoryAnalysisSettingsStore;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import com.archanchor.services.architecturalAnalyses.ArchitectureViolation;
import com.archanchor.services.architecturalAnalyses.MergeModule;
import com.archanchor.services.architecturalAnalyses.MoveClass;
import com.archanchor.services.architecturalAnalyses.SplitModule;
import com.archanchor.services.parsers.EveryLanguage;
import com.archanchor.services.rating.ActiveRatingWeights;
import com.archanchor.services.rating.InMemoryRatingWeightsStore;

import java.util.List;

/**
 * The analysis wired by hand as the application wires it, with the real parser of every
 * language, for tests that analyse a project from its sources. Weights, settings and the
 * saved modules live in memory, so nothing of whoever runs the tests is read or written.
 *
 * <pre>
 * HandWiredAnalysis analysis = new HandWiredAnalysis();
 * ProjectAnalysesDTO bookstore = analysis.projectService.analyzeProject("bookstore", "../examples/bookstore");
 * </pre>
 */
public final class HandWiredAnalysis {

	public final InMemoryRatingWeightsStore ratingWeightsStore = new InMemoryRatingWeightsStore();

	public final InMemoryAnalysisSettingsStore analysisSettingsStore = new InMemoryAnalysisSettingsStore();

	public final ProjectService projectService;

	public final RecommendationsService recommendationsService;

	public HandWiredAnalysis() {
		ActiveRatingWeights activeWeights = new ActiveRatingWeights();
		ActiveAnalysisSettings activeSettings = new ActiveAnalysisSettings();
		InMemoryModuleService moduleService = new InMemoryModuleService(EveryLanguage.parsers(), activeWeights);
		SplitModule splitModule = new SplitModule(moduleService, activeWeights, activeSettings);
		MergeModule mergeModule = new MergeModule(moduleService, activeWeights, activeSettings);
		MoveClass moveClass = new MoveClass(moduleService, activeWeights, activeSettings);
		ArchitectureViolation architectureViolation = new ArchitectureViolation(moduleService, activeWeights,
				activeSettings);
		// Same order as the running application: Spring registers the analyses by class
		// name.
		projectService = new ProjectService(moduleService,
				new ArchitecturalAnalysesRunner(List.of(architectureViolation, mergeModule, moveClass, splitModule)),
				ratingWeightsStore, activeWeights, analysisSettingsStore, activeSettings);
		recommendationsService = new RecommendationsService(splitModule, mergeModule, moveClass, architectureViolation,
				activeSettings);
	}

}
