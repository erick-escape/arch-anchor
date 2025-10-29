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
        int module1Violations;
        int module2Violations;
        int mergedViolations;
        double similarityImprovement;
        double violationsImprovement;
        double rate;

        public MergeModuleResult(String module1Id, String module1Name, String module2Id,
                                 String module2Name, double module1AvgRefSim,
                                 double module2AvgRefSim, double mergedAvgRefSim,
                                 int module1Violations, int module2Violations, int mergedViolations,
                                 double similarityImprovement, double violationsImprovement, double rate) {
            this.module1Id = module1Id;
            this.module1Name = module1Name;
            this.module2Id = module2Id;
            this.module2Name = module2Name;
            this.module1AvgRefSim = module1AvgRefSim;
            this.module2AvgRefSim = module2AvgRefSim;
            this.mergedAvgRefSim = mergedAvgRefSim;
            this.module1Violations = module1Violations;
            this.module2Violations = module2Violations;
            this.mergedViolations = mergedViolations;
            this.similarityImprovement = similarityImprovement;
            this.violationsImprovement = violationsImprovement;
            this.rate = rate;
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
                0.0,
                0 // violations will be calculated later
        );
    }

    /**
     * Calculates the rate for merging two modules by considering both similarity and violations improvements.
     * Uses normalized deltas for similarity and violations, weighted equally.
     *
     * @return A MergeRateResult containing the rate and detailed metrics (rate can be positive or negative)
     */
    private MergeRateResult calculateModuleMergeRate(Module module1, Module module2, Module mergedModule) {
        this.moduleService.calculateAvgSimilarityWithRefClazzes(mergedModule);
        this.moduleService.calculateModuleSimilarity(mergedModule);
        this.moduleService.populateRefClazzesDependencies(mergedModule);
        this.moduleService.populateModuleDependencies(mergedModule);
        this.moduleService.calculateModuleViolations(mergedModule);

        // Get similarity values
        double module1Similarity = module1.getSimilarity() != null ? module1.getSimilarity() : 0.0;
        double module2Similarity = module2.getSimilarity() != null ? module2.getSimilarity() : 0.0;
        double mergedSimilarity = mergedModule.getSimilarity() != null ? mergedModule.getSimilarity() : 0.0;

        // Calculate similarity improvement (already normalized as similarity is [0, 1])
        double avgOriginalSimilarity = (module1Similarity + module2Similarity) / 2.0;
        double similarityImprovement = mergedSimilarity - avgOriginalSimilarity;

        // Get violations values
        int module1Violations = module1.getViolations() != null ? module1.getViolations() : 0;
        int module2Violations = module2.getViolations() != null ? module2.getViolations() : 0;
        int mergedViolations = mergedModule.getViolations() != null ? mergedModule.getViolations() : 0;

        // Calculate violations improvement (normalize by before state)
        int totalOriginalViolations = module1Violations + module2Violations;
        double violationsReduction = totalOriginalViolations - mergedViolations;
        double normalizedViolationsImprovement = violationsReduction / Math.max(totalOriginalViolations, 1.0);

        // Calculate rate using weighted combination
        double rate = (ModuleService.SIMILARITY_WEIGHT * similarityImprovement) +
                      (ModuleService.VIOLATION_WEIGHT * normalizedViolationsImprovement);

        return new MergeRateResult(module1Violations, module2Violations, mergedViolations,
                                   similarityImprovement, normalizedViolationsImprovement, rate);
    }

    private static class MergeRateResult {
        int module1Violations;
        int module2Violations;
        int mergedViolations;
        double similarityImprovement;
        double violationsImprovement;
        double rate;

        public MergeRateResult(int module1Violations, int module2Violations, int mergedViolations,
                               double similarityImprovement, double violationsImprovement, double rate) {
            this.module1Violations = module1Violations;
            this.module2Violations = module2Violations;
            this.mergedViolations = mergedViolations;
            this.similarityImprovement = similarityImprovement;
            this.violationsImprovement = violationsImprovement;
            this.rate = rate;
        }
    }

    public void mergeModuleAnalysis(List<Module> modules) {
        log.info("Starting merge module analysis...");

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

                MergeRateResult rateResult = calculateModuleMergeRate(module1, module2, mergedModule);

                // Only recommend merge if rate is positive (considering both similarity and violations)
                if (rateResult.rate > 0) {
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
                            rateResult.module1Violations,
                            rateResult.module2Violations,
                            rateResult.mergedViolations,
                            rateResult.similarityImprovement,
                            rateResult.violationsImprovement,
                            rateResult.rate
                    ));

                    log.debug("Potential merge: '{}' with '{}' - Rate: {}, Similarity improvement: {}, Violations improvement: {}",
                            module1.getName(), module2.getName(), rateResult.rate,
                            rateResult.similarityImprovement, rateResult.violationsImprovement);
                }
            }
        }

        potentialMerges.sort((a, b) -> Double.compare(b.rate, a.rate));

        log.info("Found {} potential beneficial merges", potentialMerges.size());
        if (potentialMerges.isEmpty()) {
            log.info("No beneficial merge suggestions found. All modules appear to be optimally separated.");
        } else {
            log.info("Top merge suggestions:");
            for (int i = 0; i < Math.min(5, potentialMerges.size()); i++) {
                MergeModuleResult merge = potentialMerges.get(i);
                log.info("  {}. Merge module '{}' with module '{}'",
                        (i + 1), merge.module1Name, merge.module2Name);
                log.info("      Rate: +{} (Similarity: {}, Violations: {} → {})",
                        String.format("%.4f", merge.rate),
                        String.format("%.4f", merge.similarityImprovement),
                        (merge.module1Violations + merge.module2Violations),
                        merge.mergedViolations);
            }
        }
    }
}