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
public class SplitModule extends ArchitecturalAnalysis {
    public SplitModule(ModuleService moduleService) {
        super(moduleService);
    }

    @Override
    public void execute(List<Module> modules) {
        splitModuleAnalysis(modules);
    }

    private static class SplitModuleResult {
        String originalModuleId;
        String originalModuleName;
        String module1Id;
        String module1Name;
        String module2Id;
        String module2Name;
        String superRefClassName;
        String deRefClassName;
        List<String> module1ClassNames;
        List<String> module2ClassNames;
        double originalSimilarity;
        double module1Similarity;
        double module2Similarity;
        double improvement;

        public SplitModuleResult(String originalModuleId, String originalModuleName,
                                 String module1Id, String module1Name,
                                 String module2Id, String module2Name,
                                 String superRefClassName, String deRefClassName,
                                 List<String> module1ClassNames, List<String> module2ClassNames,
                                 double originalSimilarity, double module1Similarity,
                                 double module2Similarity, double improvement) {
            this.originalModuleId = originalModuleId;
            this.originalModuleName = originalModuleName;
            this.module1Id = module1Id;
            this.module1Name = module1Name;
            this.module2Id = module2Id;
            this.module2Name = module2Name;
            this.superRefClassName = superRefClassName;
            this.deRefClassName = deRefClassName;
            this.module1ClassNames = new ArrayList<>(module1ClassNames);
            this.module2ClassNames = new ArrayList<>(module2ClassNames);
            this.originalSimilarity = originalSimilarity;
            this.module1Similarity = module1Similarity;
            this.module2Similarity = module2Similarity;
            this.improvement = improvement;
        }
    }

    /**
     * Finds the reference class with the highest avgSimilarityWithRefClazzes (superRefClass)
     */
    private Clazz findSuperRefClass(Module module) {
        if (module.getRefClazzes() == null || module.getRefClazzes().isEmpty()) {
            return null;
        }

        return module.getRefClazzes().stream()
                .max((c1, c2) -> {
                    double sim1 = c1.getAvgSimilarityWithRefClazzes() != null ? c1.getAvgSimilarityWithRefClazzes() : 0.0;
                    double sim2 = c2.getAvgSimilarityWithRefClazzes() != null ? c2.getAvgSimilarityWithRefClazzes() : 0.0;
                    return Double.compare(sim1, sim2);
                })
                .orElse(null);
    }

    /**
     * Finds the non-reference class with the lowest avgSimilarityWithRefClazzes (deRefClass)
     */
    private Clazz findDeRefClass(Module module) {
        List<Clazz> refClazzes = module.getRefClazzes() != null ? module.getRefClazzes() : new ArrayList<>();

        return module.getClazzes().stream()
                .filter(clazz -> !refClazzes.contains(clazz)) // Exclude reference classes
                .min((c1, c2) -> {
                    double sim1 = c1.getAvgSimilarityWithRefClazzes() != null ? c1.getAvgSimilarityWithRefClazzes() : 0.0;
                    double sim2 = c2.getAvgSimilarityWithRefClazzes() != null ? c2.getAvgSimilarityWithRefClazzes() : 0.0;
                    return Double.compare(sim1, sim2);
                })
                .orElse(null);
    }

    /**
     * Assigns each class to either module1 (superRefClass side) or module2 (deRefClass side)
     * based on which edge it is closer to
     */
    private void assignClassesToModules(Module originalModule, Clazz superRefClass, Clazz deRefClass,
                                        List<Clazz> module1Classes, List<Clazz> module2Classes) {
        // Add the reference edges to their respective modules
        module1Classes.add(superRefClass);
        module2Classes.add(deRefClass);

        // Assign remaining classes based on proximity
        for (Clazz clazz : originalModule.getClazzes()) {
            if (clazz.equals(superRefClass) || clazz.equals(deRefClass)) {
                continue; // Already assigned
            }

            double similarityToSuper = this.moduleService.calculateSimilarity(clazz, superRefClass);
            double similarityToDeRef = this.moduleService.calculateSimilarity(clazz, deRefClass);

            if (similarityToSuper >= similarityToDeRef) {
                module1Classes.add(clazz);
            } else {
                module2Classes.add(clazz);
            }
        }
    }

    /**
     * Creates a new module with specified classes and reference class
     */
    private Module createSplitModule(Module originalModule, List<Clazz> classes, Clazz refClass, String suffix) {
        String newId = originalModule.getId() + "_" + suffix;
        String newName = originalModule.getName() + " (" + suffix + ")";

        return new Module(
                newId,
                newName,
                List.of(refClass), // Set the edge class as reference
                null,
                null,
                new ArrayList<>(classes),
                0.0,
                0.0
        );
    }

    /**
     * Evaluates if splitting improves module cohesion.
     * Both split modules must have higher similarity than the original module.
     */
    private boolean isSplitBeneficial(Module original, Module module1, Module module2) {
        // Calculate all similarities for module1
        this.moduleService.calculateClassSimilarities(module1);
        this.moduleService.calculateAvgSimilarityWithRefClazzes(module1);
        this.moduleService.calculateModuleSimilarity(module1);

        // Calculate all similarities for module2
        this.moduleService.calculateClassSimilarities(module2);
        this.moduleService.calculateAvgSimilarityWithRefClazzes(module2);
        this.moduleService.calculateModuleSimilarity(module2);

        double originalSimilarity = original.getSimilarity();
        double module1Similarity = module1.getSimilarity();
        double module2Similarity = module2.getSimilarity();

        return module1Similarity > originalSimilarity && module2Similarity > originalSimilarity;
    }

    /**
     * Calculates the minimum improvement across both split modules
     */
    private double calculateImprovement(Module original, Module module1, Module module2) {
        double originalSimilarity = original.getSimilarity();
        double module1Similarity = module1.getSimilarity();
        double module2Similarity = module2.getSimilarity();

        double improvement1 = module1Similarity - originalSimilarity;
        double improvement2 = module2Similarity - originalSimilarity;

        return (improvement1 + improvement2) / 2;
    }

    public void splitModuleAnalysis(List<Module> modules) {
        log.info("Starting edge-based split module analysis for {} modules...", modules.size());

        List<SplitModuleResult> potentialSplits = new ArrayList<>();

        for (Module module : modules) {
            // Skip modules without reference classes
            if (module.getRefClazzes() == null || module.getRefClazzes().isEmpty()) {
                log.debug("Skipping module '{}' - no reference classes defined", module.getName());
                continue;
            }

            // Need at least 3 classes: 1 reference class, 1 non-reference class to form edges, and 1 to split
            if (module.getClazzes().size() < 3) {
                log.debug("Skipping module '{}' - insufficient classes for splitting ({})",
                        module.getName(), module.getClazzes().size());
                continue;
            }

            // Find the two edge classes
            Clazz superRefClass = findSuperRefClass(module);
            Clazz deRefClass = findDeRefClass(module);

            if (superRefClass == null || deRefClass == null) {
                log.debug("Skipping module '{}' - could not identify edge classes", module.getName());
                continue;
            }

            // Prevent splitting if edge classes are the same
            if (superRefClass.equals(deRefClass)) {
                log.debug("Skipping module '{}' - edge classes are identical", module.getName());
                continue;
            }

            log.debug("Analyzing split for module '{}' with {} classes. SuperRefClass: '{}', DeRefClass: '{}'",
                    module.getName(), module.getClazzes().size(), superRefClass.getName(), deRefClass.getName());

            // Assign classes to modules based on proximity to edges
            List<Clazz> module1Classes = new ArrayList<>();
            List<Clazz> module2Classes = new ArrayList<>();
            assignClassesToModules(module, superRefClass, deRefClass, module1Classes, module2Classes);

            // Both modules must have at least one class
            if (module1Classes.isEmpty() || module2Classes.isEmpty()) {
                log.debug("Skipping split - one of the split modules would be empty");
                continue;
            }

            // Create the two split modules
            Module module1 = createSplitModule(module, module1Classes, superRefClass, "high-cohesion");
            Module module2 = createSplitModule(module, module2Classes, deRefClass, "low-cohesion");

            // Evaluate if split is beneficial
            if (isSplitBeneficial(module, module1, module2)) {
                double improvement = calculateImprovement(module, module1, module2);

                List<String> module1ClassNames = module1Classes.stream()
                        .map(Clazz::getName)
                        .collect(Collectors.toList());

                List<String> module2ClassNames = module2Classes.stream()
                        .map(Clazz::getName)
                        .collect(Collectors.toList());

                potentialSplits.add(new SplitModuleResult(
                        module.getId(),
                        module.getName(),
                        module1.getId(),
                        module1.getName(),
                        module2.getId(),
                        module2.getName(),
                        superRefClass.getName(),
                        deRefClass.getName(),
                        module1ClassNames,
                        module2ClassNames,
                        module.getSimilarity(),
                        module1.getSimilarity(),
                        module2.getSimilarity(),
                        improvement
                ));

                log.debug("Beneficial split found for '{}': Module1 similarity: {}, Module2 similarity: {}, Min improvement: {}",
                        module.getName(), module1.getSimilarity(), module2.getSimilarity(), improvement);
            }
        }

        potentialSplits.sort((a, b) -> Double.compare(b.improvement, a.improvement));

        log.info("Found {} potential beneficial splits", potentialSplits.size());
        if (potentialSplits.isEmpty()) {
            log.info("No beneficial split suggestions found. All modules appear to have optimal cohesion.");
        } else {
            log.info("Top split suggestions (based on minimum improvement):");
            for (int i = 0; i < Math.min(5, potentialSplits.size()); i++) {
                SplitModuleResult split = potentialSplits.get(i);
                log.info("  {}. Split module '{}'", (i + 1), split.originalModuleName);
                log.info("     Original similarity: {}", String.format("%.4f", split.originalSimilarity));
                log.info("     → Module 1 '{}' (ref: '{}'): {} classes, similarity: {}",
                        split.module1Name, split.superRefClassName, split.module1ClassNames.size(),
                        String.format("%.4f", split.module1Similarity));
                log.info("     → Module 2 '{}' (ref: '{}'): {} classes, similarity: {}",
                        split.module2Name, split.deRefClassName, split.module2ClassNames.size(),
                        String.format("%.4f", split.module2Similarity));
                log.info("     Min improvement: +{}", String.format("%.4f", split.improvement));
            }
        }
    }
}