package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;
import com.archanchor.services.rating.ActiveRatingWeights;

import java.util.List;
import java.util.function.Supplier;

public abstract class ArchitecturalAnalysis {

	protected final ModuleService moduleService;

	private final ActiveRatingWeights activeWeights;

	public ArchitecturalAnalysis(ModuleService moduleService, ActiveRatingWeights activeWeights) {
		this.moduleService = moduleService;
		this.activeWeights = activeWeights;
	}

	public abstract void execute(List<Module> modules);

	/**
	 * Rates a structural change with the recommendation weights the architect set for the
	 * project, 0.5 each by default (#27). Only a positive rate is worth recommending.
	 *
	 * <pre>
	 * double rate = recommendationRate(similarityImprovement, normalizedViolationsImprovement);
	 * </pre>
	 */
	protected double recommendationRate(double similarityImprovement, double violationsImprovement) {
		return activeWeights.current().recommendation().rate(similarityImprovement, violationsImprovement);
	}

	/**
	 * Runs a what-if computation over hypothetical modules built from the classes of
	 * {@code realModules}, then puts those classes' similarity metrics back. Without
	 * this, the next analysis and the API response read values a simulation left behind
	 * instead of the project's own (#16).
	 *
	 * <pre>
	 * SplitRateResult rate = simulateOn(List.of(module), () -&gt; calculateSplitRate(module, half1, half2));
	 * </pre>
	 */
	protected <T> T simulateOn(List<Module> realModules, Supplier<T> whatIf) {
		List<Clazz> sharedClazzes = realModules.stream().flatMap(module -> module.getClazzes().stream()).toList();
		ClazzMetricsSnapshot asAnalysed = ClazzMetricsSnapshot.of(sharedClazzes);
		try {
			return whatIf.get();
		}
		finally {
			asAnalysed.restore();
		}
	}

}
