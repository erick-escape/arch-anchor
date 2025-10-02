package tcc.com.viewer.services.architecturalAnalyses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.services.ModuleService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MoveClass extends ArchitecturalAnalysis {
    public MoveClass(ModuleService moduleService) {
        super(moduleService);
    }

    @Override
    public void execute(List<Module> modules) {
        moveClassAnalysis(modules);
    }

    private static class MoveClassResult {
        String sourceModuleId;
        String targetModuleId;
        String classId;
        String className;
        double currentAvgSimilarity;
        double targetAvgSimilarity;
        double improvement;

        public MoveClassResult(String sourceModuleId, String targetModuleId, String classId,
                               String className, double currentAvgSimilarity,
                               double targetAvgSimilarity, double improvement) {
            this.sourceModuleId = sourceModuleId;
            this.targetModuleId = targetModuleId;
            this.classId = classId;
            this.className = className;
            this.currentAvgSimilarity = currentAvgSimilarity;
            this.targetAvgSimilarity = targetAvgSimilarity;
            this.improvement = improvement;
        }
    }

    /**
     * Calculates the improvement in avgSimilarityWithRefClazzes for a class when moved
     * from source module to target module. A positive improvement indicates that the class
     * would have better cohesion with the target module's reference classes.
     *
     * @param classToMove The class being evaluated for relocation
     * @param sourceModule The module currently containing the class
     * @param targetModule The module to potentially receive the class
     * @return The improvement value (target similarity - current similarity), or 0 if no improvement
     */
    private double calculateAvgSimilarityImprovement(Clazz classToMove, Module sourceModule, Module targetModule) {
        // Get current avgSimilarityWithRefClazzes in the source module
        double currentAvgSimilarity = classToMove.getAvgSimilarityWithRefClazzes() != null
                ? classToMove.getAvgSimilarityWithRefClazzes()
                : 0.0;

        // Calculate what the avgSimilarityWithRefClazzes would be in the target module
        double targetAvgSimilarity = 0.0;
        if (targetModule.getRefClazzes() != null && !targetModule.getRefClazzes().isEmpty()) {
            targetAvgSimilarity = this.moduleService.calculateAvgSimilarityWithRefClazzes(
                    classToMove,
                    targetModule.getRefClazzes()
            );
        }

        // Calculate improvement
        double improvement = targetAvgSimilarity - currentAvgSimilarity;

        return improvement;
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

            for (Clazz classToMove : sourceModule.getClazzes()) {
                double currentAvgSimilarity = classToMove.getAvgSimilarityWithRefClazzes() != null
                        ? classToMove.getAvgSimilarityWithRefClazzes()
                        : 0.0;

                for (int j = 0; j < modules.size(); j++) {
                    if (i == j) continue;

                    Module targetModule = modules.get(j);

                    // Skip target modules without reference classes
                    if (targetModule.getRefClazzes() == null || targetModule.getRefClazzes().isEmpty()) {
                        continue;
                    }

                    // Calculate improvement based on avgSimilarityWithRefClazzes
                    double improvement = calculateAvgSimilarityImprovement(classToMove, sourceModule, targetModule);

                    // Only consider moves that improve the class's similarity with reference classes
                    if (improvement > 0) {
                        double targetAvgSimilarity = currentAvgSimilarity + improvement;

                        potentialMoves.add(new MoveClassResult(
                                sourceModule.getId(),
                                targetModule.getId(),
                                classToMove.getId(),
                                classToMove.getName(),
                                currentAvgSimilarity,
                                targetAvgSimilarity,
                                improvement
                        ));

                        log.debug("Potential move: '{}' from '{}' to '{}' - Current: {}, Target: {}, Improvement: {}",
                                classToMove.getName(), sourceModule.getName(), targetModule.getName(),
                                currentAvgSimilarity, targetAvgSimilarity, improvement);
                    }
                }
            }
        }

        potentialMoves.sort((a, b) -> Double.compare(b.improvement, a.improvement));

        log.info("Found {} potential beneficial moves", potentialMoves.size());
        if (potentialMoves.isEmpty()) {
            log.info("No beneficial move class suggestions found. All classes appear to be optimally placed.");
        } else {
            log.info("Top move suggestions (based on avgSimilarityWithRefClazzes improvement):");
            for (int i = 0; i < Math.min(5, potentialMoves.size()); i++) {
                MoveClassResult move = potentialMoves.get(i);
                log.info("  {}. Move '{}' from module '{}' to module '{}'",
                        (i + 1), move.className, move.sourceModuleId, move.targetModuleId);
                log.info("     Current avgSimilarity: {}, Target avgSimilarity: {}, Improvement: +{}",
                        String.format("%.4f", move.currentAvgSimilarity),
                        String.format("%.4f", move.targetAvgSimilarity),
                        String.format("%.4f", move.improvement));
            }
        }
    }
}
