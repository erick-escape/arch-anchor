package tcc.com.viewer.services.architecturalAnalyses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.services.ModuleService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MergeModule extends ArchitecturalAnalysis {
    public MergeModule(ModuleService moduleService) {
        super(moduleService);
    }

    @Override
    public void execute(List<Module> modules) {
        mergeModuleAnalysis(modules);
    }

    private static class MergeModuleResult {
        String module1Id;
        String module1Name;
        String module2Id;
        String module2Name;
        double improvement;

        public MergeModuleResult(String module1Id, String module1Name, String module2Id,
                                 String module2Name, double improvement) {
            this.module1Id = module1Id;
            this.module1Name = module1Name;
            this.module2Id = module2Id;
            this.module2Name = module2Name;
            this.improvement = improvement;
        }
    }

    private Module createMergedModule(Module module1, Module module2) {
        List<Clazz> mergedClazzes = new ArrayList<>(module1.getClazzes());
        mergedClazzes.addAll(module2.getClazzes());

//        List<Clazz> mergedRefClazzes = new ArrayList<>(module1.getRefClazzes());
//        mergedRefClazzes.addAll(module2.getRefClazzes());
//
//        List<AllowedRule> mergedAllowedRules = new ArrayList<>(module1.getAllowedRules());
//        mergedAllowedRules.addAll(module2.getAllowedRules());

        String mergedId = module1.getId() + "_" + module2.getId();
        String mergedName = module1.getName() + " + " + module2.getName();

        // we don't need refClazzes, allowedRules, allDependenciesOrigin, refClazzesDependencies or allDependencies to calculate the improvement of the merge
        return new Module(
                mergedId,
                mergedName,
                null,
                null,
                null,
                null,
                null,
                mergedClazzes,
                0.0
        );
    }

    private double calculateSimilarityImprovement(Module module1, Module module2, Module mergedModule) {
        this.moduleService.calculateClassSimilarities(mergedModule);
        this.moduleService.calculateModuleSimilarity(mergedModule);

        double originalSimilarity1 = module1.getSimilarity();
        double originalSimilarity2 = module2.getSimilarity();
        double mergedSimilarity = mergedModule.getSimilarity();

        double originalCombinedSimilarity = (originalSimilarity1 + originalSimilarity2) / 2.0;

        return mergedSimilarity - originalCombinedSimilarity;
    }

    public void mergeModuleAnalysis(List<Module> modules) {
        log.info("Starting merge module analysis...");

        List<MergeModuleResult> potentialMerges = new ArrayList<>();

        for (int i = 0; i < modules.size(); i++) {
            Module module1 = modules.get(i);

            for (int j = i + 1; j < modules.size(); j++) {
                Module module2 = modules.get(j);

                Module mergedModule = createMergedModule(module1, module2);

                double improvement = calculateSimilarityImprovement(module1, module2, mergedModule);

                if (improvement > 0) {
                    potentialMerges.add(new MergeModuleResult(
                            module1.getId(),
                            module1.getName(),
                            module2.getId(),
                            module2.getName(),
                            improvement
                    ));
                }
            }
        }

        potentialMerges.sort((a, b) -> Double.compare(b.improvement, a.improvement));

        log.info("Found {} potential beneficial merges", potentialMerges.size());
        for (int i = 0; i < Math.min(5, potentialMerges.size()); i++) {
            MergeModuleResult merge = potentialMerges.get(i);
            log.info("Merge suggestion: Module '{}' with module '{}' - Improvement: {}",
                    merge.module1Name, merge.module2Name, merge.improvement);
        }
    }
}