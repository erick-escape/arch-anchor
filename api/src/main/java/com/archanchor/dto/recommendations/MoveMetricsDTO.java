package com.archanchor.dto.recommendations;

import java.io.Serializable;

public record MoveMetricsDTO(double classCurrentSimilarity, double classTargetSimilarity, int sourceViolationsBefore,
		int sourceViolationsAfter, int targetViolationsBefore, int targetViolationsAfter, double similarityImprovement,
		double violationsImprovement, double rate) implements Serializable {
}
