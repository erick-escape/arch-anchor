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
        double module1AvgRefSim;
        double module2AvgRefSim;
        double mergedAvgRefSim;
        double improvement;

        public MergeModuleResult(String module1Id, String module1Name, String module2Id,
                                 String module2Name, double module1AvgRefSim,
                                 double module2AvgRefSim, double mergedAvgRefSim, double improvement) {
            this.module1Id = module1Id;
            this.module1Name = module1Name;
            this.module2Id = module2Id;
            this.module2Name = module2Name;
            this.module1AvgRefSim = module1AvgRefSim;
            this.module2AvgRefSim = module2AvgRefSim;
            this.mergedAvgRefSim = mergedAvgRefSim;
            this.improvement = improvement;
        }
    }

    /**
     * Creates a hypothetical merged module by combining all classes from both modules
     * and merging their reference classes.
     */
    private Module createMergedModule(Module module1, Module module2) {
        List<Clazz> mergedClazzes = new ArrayList<>(module1.getClazzes());
        mergedClazzes.addAll(module2.getClazzes());

        // Merge reference classes from both modules
        List<Clazz> mergedRefClazzes = new ArrayList<>();
        if (module1.getRefClazzes() != null) {
            mergedRefClazzes.addAll(module1.getRefClazzes());
        }
        if (module2.getRefClazzes() != null) {
            mergedRefClazzes.addAll(module2.getRefClazzes());
        }

        String mergedId = module1.getId() + "_" + module2.getId();
        String mergedName = module1.getName() + " + " + module2.getName();

        return new Module(
                mergedId,
                mergedName,
                mergedRefClazzes,
                null, // refClazzesDependencies not needed for this analysis
                null, // moduleDependencies not needed for this analysis
                mergedClazzes,
                0.0,
                0.0
        );
    }

    /**
     * Calculates the improvement in avgRefClazzesSimilarity when merging two modules.
     * The improvement is calculated by comparing the merged module's avgRefClazzesSimilarity
     * with both original modules' avgRefClazzesSimilarity values.
     *
     * @return The minimum improvement (positive if merged is better than both originals)
     */
    private double calculateAvgRefClazzesSimilarityImprovement(Module module1, Module module2, Module mergedModule) {
        // Calculate avgSimilarityWithRefClazzes for each reference class in the merged module
        this.moduleService.calculateAvgSimilarityWithRefClazzes(mergedModule);

        // Calculate the merged module's avgRefClazzesSimilarity
        List<Clazz> mergedRefClazzes = mergedModule.getRefClazzes();
        double mergedAvgRefClazzesSimilarity = 0.0;

        if (mergedRefClazzes != null && !mergedRefClazzes.isEmpty()) {
            double totalRefSimilarity = 0.0;
            for (Clazz refClazz : mergedRefClazzes) {
                if (refClazz.getAvgSimilarityWithRefClazzes() != null) {
                    totalRefSimilarity += refClazz.getAvgSimilarityWithRefClazzes();
                }
            }
            mergedAvgRefClazzesSimilarity = totalRefSimilarity / mergedRefClazzes.size();
        }

        mergedModule.setAvgRefClazzesSimilarity(mergedAvgRefClazzesSimilarity);

        double originalAvgRefSim1 = module1.getAvgRefClazzesSimilarity() != null ? module1.getAvgRefClazzesSimilarity() : 0.0;
        double originalAvgRefSim2 = module2.getAvgRefClazzesSimilarity() != null ? module2.getAvgRefClazzesSimilarity() : 0.0;

        // The improvement must be positive for BOTH modules (merged must be better than both)
        double improvement1 = mergedAvgRefClazzesSimilarity - originalAvgRefSim1;
        double improvement2 = mergedAvgRefClazzesSimilarity - originalAvgRefSim2;

        // Return the minimum improvement (both must be positive for a valid merge)
        return Math.min(improvement1, improvement2);
    }

    public void mergeModuleAnalysis(List<Module> modules) {
        log.info("Starting merge module analysis based on avgRefClazzesSimilarity...");

        List<MergeModuleResult> potentialMerges = new ArrayList<>();

        for (int i = 0; i < modules.size(); i++) {
            Module module1 = modules.get(i);

            // Skip modules without reference classes
            if (module1.getRefClazzes() == null || module1.getRefClazzes().isEmpty()) {
                log.debug("Skipping module '{}' - no reference classes defined", module1.getName());
                continue;
            }

            for (int j = i + 1; j < modules.size(); j++) {
                Module module2 = modules.get(j);

                // Skip modules without reference classes
                if (module2.getRefClazzes() == null || module2.getRefClazzes().isEmpty()) {
                    continue;
                }

                Module mergedModule = createMergedModule(module1, module2);

                // Skip if merged module has no reference classes (shouldn't happen, but safety check)
                if (mergedModule.getRefClazzes() == null || mergedModule.getRefClazzes().isEmpty()) {
                    continue;
                }

                double improvement = calculateAvgRefClazzesSimilarityImprovement(module1, module2, mergedModule);

                // Only recommend merge if avgRefClazzesSimilarity improves for BOTH modules
                if (improvement > 0) {
                    double module1AvgRefSim = module1.getAvgRefClazzesSimilarity() != null ? module1.getAvgRefClazzesSimilarity() : 0.0;
                    double module2AvgRefSim = module2.getAvgRefClazzesSimilarity() != null ? module2.getAvgRefClazzesSimilarity() : 0.0;
                    double mergedAvgRefSim = mergedModule.getAvgRefClazzesSimilarity();

                    potentialMerges.add(new MergeModuleResult(
                            module1.getId(),
                            module1.getName(),
                            module2.getId(),
                            module2.getName(),
                            module1AvgRefSim,
                            module2AvgRefSim,
                            mergedAvgRefSim,
                            improvement
                    ));

                    log.debug("Potential merge: '{}' with '{}' - Module1 avgRefSim: {}, Module2 avgRefSim: {}, Merged avgRefSim: {}, Improvement: {}",
                            module1.getName(), module2.getName(), module1AvgRefSim, module2AvgRefSim, mergedAvgRefSim, improvement);
                }
            }
        }

        potentialMerges.sort((a, b) -> Double.compare(b.improvement, a.improvement));

        log.info("Found {} potential beneficial merges", potentialMerges.size());
        if (potentialMerges.isEmpty()) {
            log.info("No beneficial merge suggestions found. All modules appear to be optimally separated.");
        } else {
            log.info("Top merge suggestions (based on avgRefClazzesSimilarity improvement):");
            for (int i = 0; i < Math.min(5, potentialMerges.size()); i++) {
                MergeModuleResult merge = potentialMerges.get(i);
                log.info("  {}. Merge module '{}' with module '{}'",
                        (i + 1), merge.module1Name, merge.module2Name);
                log.info("     Module1 avgRefSim: {}, Module2 avgRefSim: {}, Merged avgRefSim: {}, Min Improvement: +{}",
                        String.format("%.4f", merge.module1AvgRefSim),
                        String.format("%.4f", merge.module2AvgRefSim),
                        String.format("%.4f", merge.mergedAvgRefSim),
                        String.format("%.4f", merge.improvement));
            }
        }
    }
}