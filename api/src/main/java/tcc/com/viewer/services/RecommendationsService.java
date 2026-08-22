package tcc.com.viewer.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tcc.com.viewer.dto.recommendations.*;
import tcc.com.viewer.services.architecturalAnalyses.ArchitectureViolation;
import tcc.com.viewer.services.architecturalAnalyses.MergeModule;
import tcc.com.viewer.services.architecturalAnalyses.MoveClass;
import tcc.com.viewer.services.architecturalAnalyses.SplitModule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationsService {

	private static final double HIGH_PRIORITY_THRESHOLD = 0.5;

	private static final double MEDIUM_PRIORITY_THRESHOLD = 0.2;

	private final SplitModule splitModule;

	private final MergeModule mergeModule;

	private final MoveClass moveClass;

	private final ArchitectureViolation architectureViolation;

	public RecommendationsService(SplitModule splitModule, MergeModule mergeModule, MoveClass moveClass,
			ArchitectureViolation architectureViolation) {
		this.splitModule = splitModule;
		this.mergeModule = mergeModule;
		this.moveClass = moveClass;
		this.architectureViolation = architectureViolation;
	}

	public RecommendationsResponseDTO getRecommendations() {
		log.info("Collecting recommendations from all analyses");

		List<SplitModuleRecommendationDTO> splits = convertSplitResults();
		List<MergeModuleRecommendationDTO> merges = convertMergeResults();
		List<MoveClassRecommendationDTO> moves = convertMoveResults();
		List<ArchitectureViolationRecommendationDTO> violations = convertViolationResults();

		RecommendationsSummaryDTO summary = calculateSummary(splits, merges, moves, violations);

		log.info("Collected {} split, {} merge, {} move, {} violation recommendations", splits.size(), merges.size(),
				moves.size(), violations.size());

		return new RecommendationsResponseDTO(splits, merges, moves, violations, summary);
	}

	private List<SplitModuleRecommendationDTO> convertSplitResults() {
		return splitModule.getResults()
			.stream()
			.map(result -> new SplitModuleRecommendationDTO(result.originalModuleId, result.originalModuleName,
					result.superRefClassName, result.deRefClassName, result.module1ClassNames, result.module2ClassNames,
					new SplitMetricsDTO(result.originalSimilarity,
							(result.module1Similarity + result.module2Similarity) / 2.0, result.originalViolations,
							result.module1Violations + result.module2Violations, result.similarityImprovement,
							result.violationsImprovement, result.rate)))
			.collect(Collectors.toList());
	}

	private List<MergeModuleRecommendationDTO> convertMergeResults() {
		return mergeModule.getResults()
			.stream()
			.map(result -> new MergeModuleRecommendationDTO(result.module1Id, result.module1Name, result.module2Id,
					result.module2Name,
					new MergeMetricsDTO(result.module1AvgRefSim, result.module2AvgRefSim, result.mergedAvgRefSim,
							result.module1Violations, result.module2Violations, result.mergedViolations,
							result.similarityImprovement, result.violationsImprovement, result.rate)))
			.collect(Collectors.toList());
	}

	private List<MoveClassRecommendationDTO> convertMoveResults() {
		return moveClass.getResults()
			.stream()
			.map(result -> new MoveClassRecommendationDTO(result.classId, result.className, result.sourceModuleId, "", // sourceModuleName
																														// not
																														// available
																														// in
																														// result
					result.targetModuleId, "", // targetModuleName not available in result
					new MoveMetricsDTO(0.0, // classCurrentSimilarity not available
							0.0, // classTargetSimilarity not available
							result.sourceViolationsBefore, result.sourceViolationsAfter, result.targetViolationsBefore,
							result.targetViolationsAfter, result.similarityImprovement, result.violationsImprovement,
							result.rate)))
			.collect(Collectors.toList());
	}

	private List<ArchitectureViolationRecommendationDTO> convertViolationResults() {
		List<ArchitectureViolationRecommendationDTO> recommendations = new ArrayList<>();

		ArchitectureViolation.ArchitectureViolationResult result = architectureViolation.getResult();

		for (ArchitectureViolation.ModuleViolation moduleViolation : result.getModuleViolations()) {
			for (ArchitectureViolation.Violation violation : moduleViolation.getViolations()) {
				for (ArchitectureViolation.ViolatingClass violatingClass : violation.getViolatingClasses()) {
					recommendations.add(new ArchitectureViolationRecommendationDTO(violatingClass.getClassId(),
							violatingClass.getClassName(), moduleViolation.getModuleId(),
							moduleViolation.getModuleName(), violation.getViolation(),
							violatingClass.getViolatingDependencyFQNs(),
							convertMoveSuggestion(violatingClass.getBestSuggestion()),
							violatingClass.getAlternativeSuggestions()
								.stream()
								.map(this::convertMoveSuggestion)
								.collect(Collectors.toList())));
				}
			}
		}

		return recommendations;
	}

	private MoveSuggestionDTO convertMoveSuggestion(ArchitectureViolation.MoveSuggestion suggestion) {
		if (suggestion == null) {
			return null;
		}

		return new MoveSuggestionDTO(suggestion.getTargetModuleId(), suggestion.getTargetModuleName(),
				suggestion.getSimilarityImprovement(), suggestion.getNewViolationsCreated(),
				suggestion.isRequiresNewModule(), suggestion.getSuggestionReason());
	}

	private RecommendationsSummaryDTO calculateSummary(List<SplitModuleRecommendationDTO> splits,
			List<MergeModuleRecommendationDTO> merges, List<MoveClassRecommendationDTO> moves,
			List<ArchitectureViolationRecommendationDTO> violations) {

		int highPriority = 0;
		int mediumPriority = 0;
		int lowPriority = 0;

		// Count priorities for split recommendations
		for (SplitModuleRecommendationDTO split : splits) {
			if (split.metrics().rate() > HIGH_PRIORITY_THRESHOLD) {
				highPriority++;
			}
			else if (split.metrics().rate() > MEDIUM_PRIORITY_THRESHOLD) {
				mediumPriority++;
			}
			else {
				lowPriority++;
			}
		}

		// Count priorities for merge recommendations
		for (MergeModuleRecommendationDTO merge : merges) {
			if (merge.metrics().rate() > HIGH_PRIORITY_THRESHOLD) {
				highPriority++;
			}
			else if (merge.metrics().rate() > MEDIUM_PRIORITY_THRESHOLD) {
				mediumPriority++;
			}
			else {
				lowPriority++;
			}
		}

		// Count priorities for move recommendations
		for (MoveClassRecommendationDTO move : moves) {
			if (move.metrics().rate() > HIGH_PRIORITY_THRESHOLD) {
				highPriority++;
			}
			else if (move.metrics().rate() > MEDIUM_PRIORITY_THRESHOLD) {
				mediumPriority++;
			}
			else {
				lowPriority++;
			}
		}

		// Violations don't have a rate, consider them all medium priority
		mediumPriority += violations.size();

		int totalRecommendations = splits.size() + merges.size() + moves.size() + violations.size();

		return new RecommendationsSummaryDTO(totalRecommendations, highPriority, mediumPriority, lowPriority);
	}

}
