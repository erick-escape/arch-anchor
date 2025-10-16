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

    private static class MoveEvaluationResult {
        boolean isValid;
        double sourceModuleSimilarityBefore;
        double sourceModuleSimilarityAfter;
        double targetModuleSimilarityBefore;
        double targetModuleSimilarityAfter;
        double classCurrentAvgSimilarity;
        double classTargetAvgSimilarity;

        public MoveEvaluationResult(boolean isValid, double sourceModuleSimilarityBefore,
                                    double sourceModuleSimilarityAfter, double targetModuleSimilarityBefore,
                                    double targetModuleSimilarityAfter, double classCurrentAvgSimilarity,
                                    double classTargetAvgSimilarity) {
            this.isValid = isValid;
            this.sourceModuleSimilarityBefore = sourceModuleSimilarityBefore;
            this.sourceModuleSimilarityAfter = sourceModuleSimilarityAfter;
            this.targetModuleSimilarityBefore = targetModuleSimilarityBefore;
            this.targetModuleSimilarityAfter = targetModuleSimilarityAfter;
            this.classCurrentAvgSimilarity = classCurrentAvgSimilarity;
            this.classTargetAvgSimilarity = classTargetAvgSimilarity;
        }
    }

    private static class MoveClassResult {
        String sourceModuleId;
        String targetModuleId;
        String classId;
        String className;
        double improvement;

        public MoveClassResult(String sourceModuleId, String targetModuleId, String classId,
                               String className, double improvement) {
            this.sourceModuleId = sourceModuleId;
            this.targetModuleId = targetModuleId;
            this.classId = classId;
            this.className = className;
            this.improvement = improvement;
        }
    }

    /**
     * Evaluates whether moving a class from source module to target module would be beneficial.
     * Uses a two-step validation:
     * 1. First, checks if the class's avgSimilarityWithRefClazzes would improve in the target module
     * 2. If yes, simulates the move and checks if:
     * - Target module's overall similarity improves with the class
     * - Source module's overall similarity doesn't decrease without the class
     *
     * @param classToMove  The class being evaluated for relocation
     * @param sourceModule The module currently containing the class
     * @param targetModule The module to potentially receive the class
     * @return MoveEvaluationResult containing validity and similarity metrics
     */
    private MoveEvaluationResult evaluateMoveClassBenefit(Clazz classToMove, Module sourceModule, Module targetModule) {
        // Step 1: Initial filter - check if class's avgSimilarityWithRefClazzes would improve
        double currentAvgSimilarity = classToMove.getAvgSimilarityWithRefClazzes() != null
                ? classToMove.getAvgSimilarityWithRefClazzes()
                : 0.0;

        double targetAvgSimilarity = 0.0;
        if (targetModule.getRefClazzes() != null && !targetModule.getRefClazzes().isEmpty()) {
            targetAvgSimilarity = this.moduleService.calculateAvgSimilarityWithRefClazzes(
                    classToMove,
                    targetModule.getRefClazzes()
            );
        }

        // If class's similarity wouldn't improve, reject the move
        if (targetAvgSimilarity <= currentAvgSimilarity) {
            return new MoveEvaluationResult(
                    false,
                    0,
                    0,
                    0,
                    0,
                    currentAvgSimilarity,
                    targetAvgSimilarity
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
        tempSourceModule.setClazzes(sourceModule.getClazzes().stream()
                .filter(c -> !c.equals(classToMove))
                .collect(Collectors.toList()));

        // Recalculate source module similarity without the class
        this.moduleService.calculateClassSimilarities(tempSourceModule);
        this.moduleService.calculateAvgSimilarityWithRefClazzes(tempSourceModule);
        this.moduleService.calculateModuleSimilarity(tempSourceModule);
        this.moduleService.populateRefClazzesDependencies(tempSourceModule);
        this.moduleService.populateModuleDependencies(tempSourceModule);
        this.moduleService.calculateModuleViolations(tempSourceModule);
        double sourceModuleSimilarityAfter = tempSourceModule.getSimilarity() != null ? tempSourceModule.getSimilarity() : 0.0;

        // Create temporary target module with the class
        Module tempTargetModule = new Module();
        tempTargetModule.setId(targetModule.getId());
        tempTargetModule.setName(targetModule.getName());
        tempTargetModule.setRefClazzes(new ArrayList<>(targetModule.getRefClazzes()));
        List<Clazz> targetClazzes = new ArrayList<>(targetModule.getClazzes());
        targetClazzes.add(classToMove);
        tempTargetModule.setClazzes(targetClazzes);

        // Recalculate target module similarity with the class
        this.moduleService.calculateClassSimilarities(tempTargetModule);
        this.moduleService.calculateAvgSimilarityWithRefClazzes(tempTargetModule);
        this.moduleService.calculateModuleSimilarity(tempTargetModule);
        this.moduleService.populateRefClazzesDependencies(tempTargetModule);
        this.moduleService.populateModuleDependencies(tempTargetModule);
        this.moduleService.calculateModuleViolations(tempTargetModule);
        double targetModuleSimilarityAfter = tempTargetModule.getSimilarity() != null ? tempTargetModule.getSimilarity() : 0.0;

        // Check if both conditions are met:
        // 1. Target module similarity improves with the class
        // 2. Source module similarity doesn't decrease without the class
        // 3. Violations don't increase in either module
        int sourceViolationsBefore = sourceModule.getViolations() != null ? sourceModule.getViolations() : 0;
        int sourceViolationsAfter = tempSourceModule.getViolations() != null ? tempSourceModule.getViolations() : 0;
        int targetViolationsBefore = targetModule.getViolations() != null ? targetModule.getViolations() : 0;
        int targetViolationsAfter = tempTargetModule.getViolations() != null ? tempTargetModule.getViolations() : 0;

        boolean isValid = (targetModuleSimilarityAfter > targetModuleSimilarityBefore) &&
                (sourceModuleSimilarityAfter >= sourceModuleSimilarityBefore) &&
                (sourceViolationsAfter <= sourceViolationsBefore) &&
                (targetViolationsAfter <= targetViolationsBefore);

        return new MoveEvaluationResult(
                isValid,
                sourceModuleSimilarityBefore,
                sourceModuleSimilarityAfter,
                targetModuleSimilarityBefore,
                targetModuleSimilarityAfter,
                currentAvgSimilarity,
                targetAvgSimilarity
        );
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
            List<Clazz> classesToConsider = sourceModule.getClazzes().stream()
                    .filter(clazz -> !sourceModule.getRefClazzes().contains(clazz))
                    .toList();

            for (Clazz classToMove : classesToConsider) {
                for (int j = 0; j < modules.size(); j++) {
                    if (i == j) continue;

                    Module targetModule = modules.get(j);

                    // Skip target modules without reference classes
                    if (targetModule.getRefClazzes() == null || targetModule.getRefClazzes().isEmpty()) {
                        continue;
                    }

                    // Evaluate if moving this class would be beneficial
                    MoveEvaluationResult evaluation = evaluateMoveClassBenefit(classToMove, sourceModule, targetModule);

                    // Only consider valid moves that benefit both modules
                    if (evaluation.isValid) {
                        // Calculate improvement as average of target and source module improvements
                        double targetImprovement = evaluation.targetModuleSimilarityAfter - evaluation.targetModuleSimilarityBefore;
                        double sourceImprovement = evaluation.sourceModuleSimilarityAfter - evaluation.sourceModuleSimilarityBefore;
                        double improvement = (targetImprovement + sourceImprovement) / 2.0;

                        potentialMoves.add(new MoveClassResult(
                                sourceModule.getId(),
                                targetModule.getId(),
                                classToMove.getId(),
                                classToMove.getName(),
                                improvement
                        ));

                        log.debug("Potential move: '{}' from '{}' to '{}' - Improvement: {}",
                                classToMove.getName(), sourceModule.getName(), targetModule.getName(), improvement);
                    }
                }
            }
        }

        // Sort by improvement (descending order)
        potentialMoves.sort((a, b) -> Double.compare(b.improvement, a.improvement));

        log.info("Found {} potential beneficial moves", potentialMoves.size());
        if (potentialMoves.isEmpty()) {
            log.info("No beneficial move class suggestions found. All classes appear to be optimally placed.");
        } else {
            log.info("Top move suggestions (based on module similarity improvements):");
            for (int i = 0; i < Math.min(5, potentialMoves.size()); i++) {
                MoveClassResult move = potentialMoves.get(i);
                log.info("  {}. Move '{}' from module '{}' to module '{}' - Improvement: +{}",
                        (i + 1), move.className, move.sourceModuleId, move.targetModuleId,
                        String.format("%.4f", move.improvement));
            }
        }
    }
}
