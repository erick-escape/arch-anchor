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

    private Module createModuleWithMovedClass(Module sourceModule, Module targetModule, Clazz classToMove) {
        List<Clazz> newClazzes = new ArrayList<>(targetModule.getClazzes());
        newClazzes.add(classToMove);

        return new Module(
                targetModule.getId(),
                targetModule.getName(),
                targetModule.getRefClazzes(),
                targetModule.getRefClazzesDependencies(),
                targetModule.getModuleDependencies(),
                newClazzes,
                0.0
        );
    }

    private Module createModuleWithRemovedClass(Module sourceModule, Clazz classToRemove) {
        List<Clazz> newClazzes = sourceModule.getClazzes().stream()
                .filter(clazz -> !clazz.getId().equals(classToRemove.getId()))
                .collect(Collectors.toList());

        return new Module(
                sourceModule.getId(),
                sourceModule.getName(),
                sourceModule.getRefClazzes(),
                sourceModule.getRefClazzesDependencies(),
                sourceModule.getModuleDependencies(),
                newClazzes,
                0.0
        );
    }

    private double calculateSimilarityImprovement(Module originalSourceModule, Module originalTargetModule,
                                                  Module newSourceModule, Module newTargetModule) {
        this.moduleService.calculateClassSimilarities(newSourceModule);
        this.moduleService.calculateModuleSimilarity(newSourceModule);
        this.moduleService.calculateClassSimilarities(newTargetModule);
        this.moduleService.calculateModuleSimilarity(newTargetModule);

        double originalSourceSimilarity = originalSourceModule.getSimilarity();
        double originalTargetSimilarity = originalTargetModule.getSimilarity();
        double newSourceSimilarity = newSourceModule.getSimilarity();
        double newTargetSimilarity = newTargetModule.getSimilarity();

        double sourceImprovement = newSourceSimilarity - originalSourceSimilarity;
        double targetImprovement = newTargetSimilarity - originalTargetSimilarity;

        if (sourceImprovement > 0 && targetImprovement > 0) {
            return sourceImprovement + targetImprovement;
        } else {
            return 0;
        }
    }

    public void moveClassAnalysis(List<Module> modules) {
        log.info("Starting move class analysis...");

        List<MoveClassResult> potentialMoves = new ArrayList<>();

        for (int i = 0; i < modules.size(); i++) {
            Module sourceModule = modules.get(i);

            for (Clazz classToMove : sourceModule.getClazzes()) {
                for (int j = 0; j < modules.size(); j++) {
                    if (i == j) continue;

                    Module targetModule = modules.get(j);

                    Module newSourceModule = createModuleWithRemovedClass(sourceModule, classToMove);
                    Module newTargetModule = createModuleWithMovedClass(sourceModule, targetModule, classToMove);

                    if (newSourceModule.getClazzes().isEmpty()) {
                        continue;
                    }

                    double improvement = calculateSimilarityImprovement(sourceModule, targetModule,
                            newSourceModule, newTargetModule);

                    if (improvement > 0) {
                        potentialMoves.add(new MoveClassResult(
                                sourceModule.getId(),
                                targetModule.getId(),
                                classToMove.getId(),
                                classToMove.getName(),
                                improvement
                        ));
                    }
                }
            }
        }

        potentialMoves.sort((a, b) -> Double.compare(b.improvement, a.improvement));

        log.info("Found {} potential beneficial moves", potentialMoves.size());
        for (int i = 0; i < Math.min(5, potentialMoves.size()); i++) {
            MoveClassResult move = potentialMoves.get(i);
            log.info("Move suggestion: Class '{}' from module '{}' to module '{}' - Improvement: {}",
                    move.className, move.sourceModuleId, move.targetModuleId, move.improvement);
        }
    }
}
