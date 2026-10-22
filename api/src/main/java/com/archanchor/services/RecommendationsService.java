package com.archanchor.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.archanchor.dto.recommendations.*;
import com.archanchor.services.analysis.ActiveAnalysisSettings;
import com.archanchor.services.architecturalAnalyses.ArchitectureViolation;
import com.archanchor.services.architecturalAnalyses.MergeModule;
import com.archanchor.services.architecturalAnalyses.MoveClass;
import com.archanchor.services.architecturalAnalyses.SplitModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class RecommendationsService {

	private final SplitModule splitModule;

	private final MergeModule mergeModule;

	private final MoveClass moveClass;

	private final ArchitectureViolation architectureViolation;

	private final ActiveAnalysisSettings activeSettings;

	public RecommendationsService(SplitModule splitModule, MergeModule mergeModule, MoveClass moveClass,
			ArchitectureViolation architectureViolation, ActiveAnalysisSettings activeSettings) {
		this.splitModule = splitModule;
		this.mergeModule = mergeModule;
		this.moveClass = moveClass;
		this.architectureViolation = architectureViolation;
		this.activeSettings = activeSettings;
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

	// Splits, merges and moves below the architect's minimum rate are not shown;
	// violation
	// entries have no rate and always are (#36). Filtered before the summary, so its
	// counts match the cards.
	private boolean isShown(double rate) {
		return activeSettings.current().shows(rate);
	}

	private List<SplitModuleRecommendationDTO> convertSplitResults() {
		return splitModule.getResults()
			.stream()
			.filter(result -> isShown(result.rate))
			.map(result -> new SplitModuleRecommendationDTO(result.originalModuleId, result.originalModuleName,
					result.superRefClassName, result.deRefClassName, result.module1ClassIds, result.module2ClassIds,
					new SplitMetricsDTO(result.originalSimilarity,
							(result.module1Similarity + result.module2Similarity) / 2.0, result.originalViolations,
							result.module1Violations + result.module2Violations, result.similarityImprovement,
							result.violationsImprovement, result.rate)))
			.collect(Collectors.toList());
	}

	private List<MergeModuleRecommendationDTO> convertMergeResults() {
		return mergeModule.getResults()
			.stream()
			.filter(result -> isShown(result.rate))
			.map(result -> new MergeModuleRecommendationDTO(result.module1Id, result.module1Name, result.module2Id,
					result.module2Name,
					new MergeMetricsDTO(result.module1Similarity, result.module2Similarity, result.mergedSimilarity,
							result.module1Violations, result.module2Violations, result.mergedViolations,
							result.similarityImprovement, result.violationsImprovement, result.rate)))
			.collect(Collectors.toList());
	}

	private List<MoveClassRecommendationDTO> convertMoveResults() {
		return moveClass.getResults()
			.stream()
			.filter(result -> isShown(result.rate))
			.map(result -> new MoveClassRecommendationDTO(result.classId, result.className, result.sourceModuleId,
					result.sourceModuleName, result.targetModuleId, result.targetModuleName, new MoveMetricsDTO(0.0, // classCurrentSimilarity
																														// not
																														// available
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
		Map<RecommendationPriority, Long> byPriority = Stream.of(splits.stream().map(split -> split.metrics().rate()),
				merges.stream().map(merge -> merge.metrics().rate()), moves.stream().map(move -> move.metrics().rate()))
			.flatMap(rates -> rates)
			.collect(Collectors.groupingBy(RecommendationPriority::of, Collectors.counting()));

		// Violations don't have a rate, consider them all medium priority
		int mediumPriority = count(byPriority, RecommendationPriority.MEDIUM) + violations.size();
		int totalRecommendations = splits.size() + merges.size() + moves.size() + violations.size();

		return new RecommendationsSummaryDTO(totalRecommendations, count(byPriority, RecommendationPriority.HIGH),
				mediumPriority, count(byPriority, RecommendationPriority.LOW));
	}

	private static int count(Map<RecommendationPriority, Long> byPriority, RecommendationPriority priority) {
		return byPriority.getOrDefault(priority, 0L).intValue();
	}

}
