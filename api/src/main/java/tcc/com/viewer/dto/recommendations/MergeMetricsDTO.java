package tcc.com.viewer.dto.recommendations;

import java.io.Serializable;

public record MergeMetricsDTO(
        double module1Similarity,
        double module2Similarity,
        double mergedSimilarity,
        int module1Violations,
        int module2Violations,
        int mergedViolations,
        double similarityImprovement,
        double violationsImprovement,
        double rate
) implements Serializable {
}
