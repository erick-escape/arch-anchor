package tcc.com.viewer.dto.recommendations;

import java.io.Serializable;

public record MoveSuggestionDTO(
        String targetModuleId,
        String targetModuleName,
        double similarityImprovement,
        int newViolationsCreated,
        boolean requiresNewModule,
        String reason
) implements Serializable {
}
