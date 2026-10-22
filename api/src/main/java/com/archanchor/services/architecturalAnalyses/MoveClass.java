package com.archanchor.services.architecturalAnalyses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.services.ModuleScope;
import com.archanchor.services.ModuleService;
import com.archanchor.services.analysis.ActiveAnalysisSettings;
import com.archanchor.services.rating.ActiveRatingWeights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MoveClass extends ArchitecturalAnalysis {

	private List<MoveClassResult> results;

	private final ActiveAnalysisSettings activeSettings;

	public MoveClass(ModuleService moduleService, ActiveRatingWeights activeWeights,
			ActiveAnalysisSettings activeSettings) {
		super(moduleService, activeWeights);
		this.activeSettings = activeSettings;
	}

	@Override
	public void execute(List<Module> modules) {
		this.results = new ArrayList<>();
		moveClassAnalysis(modules);
	}

	public List<MoveClassResult> getResults() {
		return results != null ? new ArrayList<>(results) : new ArrayList<>();
	}

	private static class MoveEvaluationResult {

		double sourceModuleSimilarityBefore;

		double sourceModuleSimilarityAfter;

		double targetModuleSimilarityBefore;

		double targetModuleSimilarityAfter;

		int sourceViolationsBefore;

		int sourceViolationsAfter;

		int targetViolationsBefore;

		int targetViolationsAfter;

		double classCurrentAvgSimilarity;

		double classTargetAvgSimilarity;

		double similarityImprovement;

		double violationsImprovement;

		double rate;

		public MoveEvaluationResult(double sourceModuleSimilarityBefore, double sourceModuleSimilarityAfter,
				double targetModuleSimilarityBefore, double targetModuleSimilarityAfter, int sourceViolationsBefore,
				int sourceViolationsAfter, int targetViolationsBefore, int targetViolationsAfter,
				double classCurrentAvgSimilarity, double classTargetAvgSimilarity, double similarityImprovement,
				double violationsImprovement, double rate) {
			this.sourceModuleSimilarityBefore = sourceModuleSimilarityBefore;
			this.sourceModuleSimilarityAfter = sourceModuleSimilarityAfter;
			this.targetModuleSimilarityBefore = targetModuleSimilarityBefore;
			this.targetModuleSimilarityAfter = targetModuleSimilarityAfter;
			this.sourceViolationsBefore = sourceViolationsBefore;
			this.sourceViolationsAfter = sourceViolationsAfter;
			this.targetViolationsBefore = targetViolationsBefore;
			this.targetViolationsAfter = targetViolationsAfter;
			this.classCurrentAvgSimilarity = classCurrentAvgSimilarity;
			this.classTargetAvgSimilarity = classTargetAvgSimilarity;
			this.similarityImprovement = similarityImprovement;
			this.violationsImprovement = violationsImprovement;
			this.rate = rate;
		}

	}

	public static class MoveClassResult {

		public String sourceModuleId;

		// Shown on the recommendation card; an id means nothing to a reader (#19).
		public String sourceModuleName = "";

		public String targetModuleName = "";

		public String targetModuleId;

		public String classId;

		public String className;

		public int sourceViolationsBefore;

		public int sourceViolationsAfter;

		public int targetViolationsBefore;

		public int targetViolationsAfter;

		// The class's similarity with its module's reference classes and with the
		// target's: a move is only considered if the second is higher, and the card shows
		// both (#72).
		public double classCurrentSimilarity;

		public double classTargetSimilarity;

		public double similarityImprovement;

		public double violationsImprovement;

		public double rate;

		public MoveClassResult(String sourceModuleId, String targetModuleId, String classId, String className,
				int sourceViolationsBefore, int sourceViolationsAfter, int targetViolationsBefore,
				int targetViolationsAfter, double similarityImprovement, double violationsImprovement, double rate) {
			this.sourceModuleId = sourceModuleId;
			this.targetModuleId = targetModuleId;
			this.classId = classId;
			this.className = className;
			this.sourceViolationsBefore = sourceViolationsBefore;
			this.sourceViolationsAfter = sourceViolationsAfter;
			this.targetViolationsBefore = targetViolationsBefore;
			this.targetViolationsAfter = targetViolationsAfter;
			this.similarityImprovement = similarityImprovement;
			this.violationsImprovement = violationsImprovement;
			this.rate = rate;
		}

	}

	/**
	 * Evaluates whether moving a class from source module to target module would be
	 * beneficial. Uses a two-step validation: 1. First, checks if the class's
	 * avgSimilarityWithRefClazzes would improve in the target module 2. If yes, simulates
	 * the move and checks if: - Target module's overall similarity improves with the
	 * class - Source module's overall similarity doesn't decrease without the class
	 * @param classToMove The class being evaluated for relocation
	 * @param sourceModule The module currently containing the class
	 * @param targetModule The module to potentially receive the class
	 * @return MoveEvaluationResult containing validity and similarity metrics
	 */
	private MoveEvaluationResult evaluateMoveClassBenefit(Clazz classToMove, Module sourceModule, Module targetModule) {
		// Step 1: Initial filter - check if class's avgSimilarityWithRefClazzes would
		// improve
		double currentAvgSimilarity = classToMove.getAvgSimilarityWithRefClazzes() != null
				? classToMove.getAvgSimilarityWithRefClazzes() : 0.0;

		double targetAvgSimilarity = 0.0;
		if (targetModule.getRefClazzes() != null && !targetModule.getRefClazzes().isEmpty()) {
			// Within the target, the class's references to its classes are internal (#43)
			targetAvgSimilarity = this.moduleService.calculateAvgSimilarityWithRefClazzes(classToMove,
					targetModule.getRefClazzes(), ModuleScope.of(targetModule).with(classToMove));
		}

		// If class's similarity wouldn't improve, reject the move with negative rate
		if (targetAvgSimilarity <= currentAvgSimilarity) {
			return new MoveEvaluationResult(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, currentAvgSimilarity, targetAvgSimilarity,
					0.0, 0.0, -1.0 // Negative rate to indicate rejection
			);
		}

		// Step 2: Simulate the move and calculate actual module similarity impacts
		double sourceModuleSimilarityBefore = sourceModule.getSimilarity() != null ? sourceModule.getSimilarity() : 0.0;
		double targetModuleSimilarityBefore = targetModule.getSimilarity() != null ? targetModule.getSimilarity() : 0.0;

		// Create temporary source module without the class
		Module tempSourceModule = new Module();
		tempSourceModule.setId(sourceModule.getId());
		tempSourceModule.setName(sourceModule.getName());
		tempSourceModule.setRefClazzes(new ArrayList<>(sourceModule.getRefClazzes()));
		tempSourceModule.setClazzes(
				sourceModule.getClazzes().stream().filter(c -> !c.equals(classToMove)).collect(Collectors.toList()));

		// Recalculate source module similarity without the class
		this.moduleService.calculateClassSimilaritiesAndSelectRefClasses(tempSourceModule);
		this.moduleService.calculateAvgSimilarityWithRefClazzes(tempSourceModule);
		this.moduleService.calculateModuleSimilarity(tempSourceModule);
		this.moduleService.populateRefClazzesDependencies(tempSourceModule);
		this.moduleService.populateModuleDependencies(tempSourceModule);
		this.moduleService.calculateModuleViolations(tempSourceModule);
		double sourceModuleSimilarityAfter = tempSourceModule.getSimilarity() != null ? tempSourceModule.getSimilarity()
				: 0.0;

		// Create temporary target module with the class
		Module tempTargetModule = new Module();
		tempTargetModule.setId(targetModule.getId());
		tempTargetModule.setName(targetModule.getName());
		tempTargetModule.setRefClazzes(new ArrayList<>(targetModule.getRefClazzes()));
		List<Clazz> targetClazzes = new ArrayList<>(targetModule.getClazzes());
		targetClazzes.add(classToMove);
		tempTargetModule.setClazzes(targetClazzes);

		// Recalculate target module similarity with the class
		this.moduleService.calculateClassSimilaritiesAndSelectRefClasses(tempTargetModule);
		this.moduleService.calculateAvgSimilarityWithRefClazzes(tempTargetModule);
		this.moduleService.calculateModuleSimilarity(tempTargetModule);
		this.moduleService.populateRefClazzesDependencies(tempTargetModule);
		this.moduleService.populateModuleDependencies(tempTargetModule);
		this.moduleService.calculateModuleViolations(tempTargetModule);
		double targetModuleSimilarityAfter = tempTargetModule.getSimilarity() != null ? tempTargetModule.getSimilarity()
				: 0.0;

		// Calculate rate by considering both similarity and violations improvements
		int sourceViolationsBefore = sourceModule.getViolations() != null ? sourceModule.getViolations() : 0;
		int sourceViolationsAfter = tempSourceModule.getViolations() != null ? tempSourceModule.getViolations() : 0;
		int targetViolationsBefore = targetModule.getViolations() != null ? targetModule.getViolations() : 0;
		int targetViolationsAfter = tempTargetModule.getViolations() != null ? tempTargetModule.getViolations() : 0;

		// Calculate similarity improvement (already normalized as similarity is [0, 1])
		double avgOriginalSimilarity = (sourceModuleSimilarityBefore + targetModuleSimilarityBefore) / 2.0;
		double avgAfterSimilarity = (sourceModuleSimilarityAfter + targetModuleSimilarityAfter) / 2.0;
		double similarityImprovement = avgAfterSimilarity - avgOriginalSimilarity;

		// Calculate violations improvement (normalize by before state)
		int totalOriginalViolations = sourceViolationsBefore + targetViolationsBefore;
		int totalAfterViolations = sourceViolationsAfter + targetViolationsAfter;
		double violationsReduction = totalOriginalViolations - totalAfterViolations;
		double normalizedViolationsImprovement = violationsReduction / Math.max(totalOriginalViolations, 1.0);

		// Weighted by the project's recommendation weights (#27)
		double rate = recommendationRate(similarityImprovement, normalizedViolationsImprovement);

		return new MoveEvaluationResult(sourceModuleSimilarityBefore, sourceModuleSimilarityAfter,
				targetModuleSimilarityBefore, targetModuleSimilarityAfter, sourceViolationsBefore,
				sourceViolationsAfter, targetViolationsBefore, targetViolationsAfter, currentAvgSimilarity,
				targetAvgSimilarity, similarityImprovement, normalizedViolationsImprovement, rate);
	}

	/**
	 * Whether a class is a candidate for a move under the architect's settings (#36): it
	 * has enough package dependencies to say where it belongs, and moving it leaves its
	 * module enough classes. A one-class module scores similarity 1 by definition, so
	 * without the second check every target shares the gain of emptying a small module.
	 */
	private boolean isJudged(Clazz clazz, Module sourceModule) {
		AnalysisSettings settings = activeSettings.current();
		int dependencies = ModuleScope.of(sourceModule).dependenciesOf(clazz).size();
		return settings.judgesClassWith(dependencies)
				&& settings.allowsMoveLeaving(sourceModule.getClazzes().size() - 1);
	}

	/**
	 * Keeps each class's best-rated targets, as many as the architect allows (#36).
	 * {@code moves} is sorted by rate, highest first, so a class's first moves are its
	 * best ones.
	 */
	private List<MoveClassResult> bestTargetsOfEachClass(List<MoveClassResult> moves) {
		AnalysisSettings settings = activeSettings.current();
		if (!settings.limitsMoveTargets()) {
			return moves;
		}
		Map<String, Integer> keptByClass = new HashMap<>();
		List<MoveClassResult> kept = new ArrayList<>();
		for (MoveClassResult move : moves) {
			int keptSoFar = keptByClass.merge(move.classId, 1, Integer::sum);
			if (keptSoFar <= settings.maxMoveTargetsPerClass()) {
				kept.add(move);
			}
		}
		return kept;
	}

	public void moveClassAnalysis(List<Module> modules) {
		log.info("Starting move class analysis based on avgSimilarityWithRefClazzes...");

		List<MoveClassResult> potentialMoves = new ArrayList<>();

		for (int i = 0; i < modules.size(); i++) {
			Module sourceModule = modules.get(i);

			// Skip modules without reference classes
			if (sourceModule.getRefClazzes() == null || sourceModule.getRefClazzes().isEmpty()) {
				log.debug("Skipping source module '{}' - no reference classes defined", sourceModule.getName());
				continue;
			}

			// Filter out reference classes - we don't want to move them
			List<Clazz> classesToConsider = sourceModule.getClazzes()
				.stream()
				.filter(clazz -> !sourceModule.getRefClazzes().contains(clazz))
				.filter(clazz -> isJudged(clazz, sourceModule))
				.toList();

			for (Clazz classToMove : classesToConsider) {
				for (int j = 0; j < modules.size(); j++) {
					if (i == j)
						continue;

					Module targetModule = modules.get(j);

					// Skip target modules without reference classes
					if (targetModule.getRefClazzes() == null || targetModule.getRefClazzes().isEmpty()) {
						continue;
					}

					// Evaluate if moving this class would be beneficial
					MoveEvaluationResult evaluation = simulateOn(List.of(sourceModule, targetModule),
							() -> evaluateMoveClassBenefit(classToMove, sourceModule, targetModule));

					// Only consider moves with positive rate (considering both
					// similarity and violations) that cost no more similarity than
					// the architect accepts (#73). Applied before each class's best
					// targets are kept, so a class whose best target costs similarity
					// keeps its best target that does not.
					if (evaluation.rate > 0 && activeSettings.current()
						.allowsMoveChangingSimilarityBy(evaluation.similarityImprovement)) {
						MoveClassResult move = new MoveClassResult(sourceModule.getId(), targetModule.getId(),
								classToMove.getId(), classToMove.getName(), evaluation.sourceViolationsBefore,
								evaluation.sourceViolationsAfter, evaluation.targetViolationsBefore,
								evaluation.targetViolationsAfter, evaluation.similarityImprovement,
								evaluation.violationsImprovement, evaluation.rate);
						move.sourceModuleName = sourceModule.getName();
						move.targetModuleName = targetModule.getName();
						move.classCurrentSimilarity = evaluation.classCurrentAvgSimilarity;
						move.classTargetSimilarity = evaluation.classTargetAvgSimilarity;
						potentialMoves.add(move);

						log.debug(
								"Potential move: '{}' from '{}' to '{}' - Rate: {}, Similarity improvement: {}, Violations improvement: {}",
								classToMove.getName(), sourceModule.getName(), targetModule.getName(), evaluation.rate,
								evaluation.similarityImprovement, evaluation.violationsImprovement);
					}
				}
			}
		}

		// Sort by rate (descending order)
		potentialMoves.sort((a, b) -> Double.compare(b.rate, a.rate));

		// Store results
		this.results = bestTargetsOfEachClass(potentialMoves);

		log.info("Found {} potential beneficial moves", potentialMoves.size());
		if (potentialMoves.isEmpty()) {
			log.info("No beneficial move class suggestions found. All classes appear to be optimally placed.");
		}
		else {
			log.info("Top move suggestions:");
			for (int i = 0; i < Math.min(5, potentialMoves.size()); i++) {
				MoveClassResult move = potentialMoves.get(i);
				log.info("  {}. Move '{}' from module '{}' to module '{}'", (i + 1), move.className,
						move.sourceModuleId, move.targetModuleId);
				log.info("      Rate: +{} (Similarity: {}, Violations: {} → {})", String.format("%.4f", move.rate),
						String.format("%.4f", move.similarityImprovement),
						(move.sourceViolationsBefore + move.targetViolationsBefore),
						(move.sourceViolationsAfter + move.targetViolationsAfter));
			}
		}
	}

}
