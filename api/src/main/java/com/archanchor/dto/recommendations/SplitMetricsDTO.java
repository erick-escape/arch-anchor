package com.archanchor.dto.recommendations;

import java.io.Serializable;

public record SplitMetricsDTO(double originalSimilarity, double avgSplitSimilarity, int originalViolations,
		int totalSplitViolations, double similarityImprovement, double violationsImprovement,
		double rate) implements Serializable {
}
