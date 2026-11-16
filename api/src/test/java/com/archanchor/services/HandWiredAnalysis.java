package com.archanchor.services;

import com.archanchor.services.analysis.ActiveAnalysisSettings;
import com.archanchor.services.analysis.InMemoryAnalysisSettingsStore;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import com.archanchor.services.architecturalAnalyses.ArchitectureViolation;
import com.archanchor.services.architecturalAnalyses.MergeModule;
import com.archanchor.services.architecturalAnalyses.MoveClass;
import com.archanchor.services.architecturalAnalyses.SplitModule;
import com.archanchor.services.parsers.JavaParser;
import com.archanchor.services.parsers.LanguageParsers;
import com.archanchor.services.rating.ActiveRatingWeights;
import com.archanchor.services.rating.InMemoryRatingWeightsStore;

import java.util.List;

/**
 * The analysis wired by hand as the application wires it, with the real parser, for tests
 * that analyse a project from its sources. Weights, settings and the saved modules live
 * in memory, so nothing of whoever runs the tests is read or written.
 *
 * <pre>
 * HandWiredAnalysis analysis = new HandWiredAnalysis();
 * ProjectAnalysesDTO bookstore = analysis.projectService.analyzeProject("bookstore", "../examples/bookstore");
 * </pre>
 */
final class HandWiredAnalysis {

	final InMemoryRatingWeightsStore ratingWeightsStore = new InMemoryRatingWeightsStore();

	final InMemoryAnalysisSettingsStore analysisSettingsStore = new InMemoryAnalysisSettingsStore();

	final ProjectService projectService;

	final RecommendationsService recommendationsService;

	HandWiredAnalysis() {
		ActiveRatingWeights activeWeights = new ActiveRatingWeights();
		ActiveAnalysisSettings activeSettings = new ActiveAnalysisSettings();
		InMemoryModuleService moduleService = new InMemoryModuleService(javaSources(), activeWeights);
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

	// A project that declares no dependencies, like the bookstore, is resolved from the
	// JDK alone, so the parser downloads nothing.
	private static LanguageParsers javaSources() {
		return new LanguageParsers(List.of(new JavaParser(new JavaParserService())));
	}

}
