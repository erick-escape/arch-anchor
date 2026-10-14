package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression suite for issue #16. The analyses simulate structural changes on modules
 * that share their {@link Clazz} instances with the real ones, and {@link ModuleService}
 * writes similarity metrics into those instances. Whatever a simulation computes must not
 * outlive it: not in the API response, and not as input to the next analysis.
 */
class AnalysesKeepClassMetricsTest {

	private final ActiveRatingWeights activeWeights = new ActiveRatingWeights();

	private final ModuleService moduleService = new ModuleService(null, activeWeights);

	static Stream<Named<BiFunction<ModuleService, ActiveRatingWeights, ArchitecturalAnalysis>>> analyses() {
		return Stream.of(analysis("SplitModule", SplitModule::new), analysis("MergeModule", MergeModule::new),
				analysis("MoveClass", MoveClass::new), analysis("ArchitectureViolation", ArchitectureViolation::new));
	}

	private static Named<BiFunction<ModuleService, ActiveRatingWeights, ArchitecturalAnalysis>> analysis(String name,
			BiFunction<ModuleService, ActiveRatingWeights, ArchitecturalAnalysis> constructor) {
		return Named.of(name, constructor);
	}

	@ParameterizedTest
	@MethodSource("analyses")
	void classMetricsAreTheSameBeforeAndAfterTheAnalysis(
			BiFunction<ModuleService, ActiveRatingWeights, ArchitecturalAnalysis> newAnalysis) {
		List<Module> modules = AnalysedShopFixture.webAndPersistence(moduleService);
		Map<String, List<Double>> asAnalysed = metricsByClassName(modules);

		newAnalysis.apply(moduleService, activeWeights).execute(modules);

		assertThat(metricsByClassName(modules)).isEqualTo(asAnalysed);
	}

	@Test
	void moveClassFindsTheSameMovesAfterSplitAndViolationAnalysisRan() {
		List<MoveClass.MoveClassResult> movesAlone = movesAfter();

		List<MoveClass.MoveClassResult> movesAfterEarlierAnalyses = movesAfter(
				new SplitModule(moduleService, activeWeights), new ArchitectureViolation(moduleService, activeWeights));

		assertThat(movesAlone).extracting(move -> move.className).containsExactly(AnalysedShopFixture.MISPLACED_CLASS);
		assertThat(movesAfterEarlierAnalyses).usingRecursiveComparison().isEqualTo(movesAlone);
	}

	@Test
	void splitStillIsolatesTheMisplacedClassAfterMoveClassRan() {
		List<Module> modules = AnalysedShopFixture.webAndPersistence(moduleService);
		SplitModule splitModule = new SplitModule(moduleService, activeWeights);

		new MoveClass(moduleService, activeWeights).execute(modules);
		splitModule.execute(modules);

		assertThat(splitModule.getResults()).singleElement().satisfies(split -> {
			assertThat(split.originalModuleName).isEqualTo(AnalysedShopFixture.WEB);
			assertThat(split.module2ClassNames).containsExactly(AnalysedShopFixture.MISPLACED_CLASS);
		});
	}

	private List<MoveClass.MoveClassResult> movesAfter(ArchitecturalAnalysis... earlierAnalyses) {
		List<Module> modules = AnalysedShopFixture.webAndPersistence(moduleService);
		Arrays.stream(earlierAnalyses).forEach(analysis -> analysis.execute(modules));
		MoveClass moveClass = new MoveClass(moduleService, activeWeights);
		moveClass.execute(modules);
		return moveClass.getResults();
	}

	private static Map<String, List<Double>> metricsByClassName(List<Module> modules) {
		Map<String, List<Double>> metrics = new LinkedHashMap<>();
		for (Module module : modules) {
			for (Clazz clazz : module.getClazzes()) {
				metrics.put(clazz.getName(), List.of(clazz.getSimilarity(), clazz.getAvgSimilarityWithRefClazzes()));
			}
		}
		return metrics;
	}

}
