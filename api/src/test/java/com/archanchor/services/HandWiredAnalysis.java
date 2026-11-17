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
import com.archanchor.services.parsers.php.ComposerAutoload;
import com.archanchor.services.parsers.php.PhpParser;
import com.archanchor.services.parsers.php.PhpPlatformClasses;
import com.archanchor.services.parsers.php.PhpSourceFiles;
import com.archanchor.services.parsers.php.TreeSitterPhpSourceReader;
import com.archanchor.services.rating.ActiveRatingWeights;
import com.archanchor.services.rating.InMemoryRatingWeightsStore;
import com.fasterxml.jackson.databind.ObjectMapper;

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
final class HandWiredAnalysis {

	final InMemoryRatingWeightsStore ratingWeightsStore = new InMemoryRatingWeightsStore();

	final InMemoryAnalysisSettingsStore analysisSettingsStore = new InMemoryAnalysisSettingsStore();

	final ProjectService projectService;

	final RecommendationsService recommendationsService;

	HandWiredAnalysis() {
		ActiveRatingWeights activeWeights = new ActiveRatingWeights();
		ActiveAnalysisSettings activeSettings = new ActiveAnalysisSettings();
		InMemoryModuleService moduleService = new InMemoryModuleService(everyLanguage(), activeWeights);
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

	// A Java project that declares no dependencies, like the bookstore, is resolved from
	// the JDK alone, so its parser downloads nothing; the PHP grammar is loaded when the
	// first PHP source is read, so a test of a Java project never needs it.
	private static LanguageParsers everyLanguage() {
		PhpParser phpParser = new PhpParser(new PhpSourceFiles(new ComposerAutoload(new ObjectMapper())),
				new TreeSitterPhpSourceReader(), new PhpPlatformClasses());
		return new LanguageParsers(List.of(new JavaParser(new JavaParserService()), phpParser));
	}

}
