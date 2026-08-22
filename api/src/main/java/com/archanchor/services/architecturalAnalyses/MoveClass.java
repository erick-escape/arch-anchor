package com.archanchor.services.architecturalAnalyses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MoveClass extends ArchitecturalAnalysis {

	private List<MoveClassResult> results;

	public MoveClass(ModuleService moduleService) {
		super(moduleService);
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

		public String targetModuleId;

		public String classId;

		public String className;

		public int sourceViolationsBefore;

		public int sourceViolationsAfter;

		public int targetViolationsBefore;

		public int targetViolationsAfter;

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
			targetAvgSimilarity = this.moduleService.calculateAvgSimilarityWithRefClazzes(classToMove,
					targetModule.getRefClazzes());
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

		// Calculate rate using weighted combination
		double rate = (ModuleService.SIMILARITY_WEIGHT * similarityImprovement)
				+ (ModuleService.VIOLATION_WEIGHT * normalizedViolationsImprovement);

		return new MoveEvaluationResult(sourceModuleSimilarityBefore, sourceModuleSimilarityAfter,
				targetModuleSimilarityBefore, targetModuleSimilarityAfter, sourceViolationsBefore,
				sourceViolationsAfter, targetViolationsBefore, targetViolationsAfter, currentAvgSimilarity,
				targetAvgSimilarity, similarityImprovement, normalizedViolationsImprovement, rate);
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
					MoveEvaluationResult evaluation = evaluateMoveClassBenefit(classToMove, sourceModule, targetModule);

					// Only consider moves with positive rate (considering both similarity
					// and violations)
					if (evaluation.rate > 0) {
						potentialMoves.add(new MoveClassResult(sourceModule.getId(), targetModule.getId(),
								classToMove.getId(), classToMove.getName(), evaluation.sourceViolationsBefore,
								evaluation.sourceViolationsAfter, evaluation.targetViolationsBefore,
								evaluation.targetViolationsAfter, evaluation.similarityImprovement,
								evaluation.violationsImprovement, evaluation.rate));

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
		this.results = potentialMoves;

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
