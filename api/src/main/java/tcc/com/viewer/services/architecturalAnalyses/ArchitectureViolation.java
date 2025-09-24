package tcc.com.viewer.services.architecturalAnalyses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.dependency.DependencyOrigin;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.domains.rules.AllowedRule;
import tcc.com.viewer.services.ModuleService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ArchitectureViolation extends ArchitecturalAnalysis {

    public ArchitectureViolation(ModuleService moduleService) {
        super(moduleService);
    }

    @Override
    public void execute(List<Module> modules) {
        architectureViolationAnalysis(modules);
    }

    private static class ArchitectureViolationResult {
        private final List<ModuleViolation> moduleViolations;
        private final List<ViolationCluster> clusters;
        private final int totalViolations;
        private final int moveableClasses;
        private final int newModulesRequired;

        public ArchitectureViolationResult(List<ModuleViolation> moduleViolations,
                                         List<ViolationCluster> clusters,
                                         int totalViolations,
                                         int moveableClasses,
                                         int newModulesRequired) {
            this.moduleViolations = new ArrayList<>(moduleViolations);
            this.clusters = new ArrayList<>(clusters);
            this.totalViolations = totalViolations;
            this.moveableClasses = moveableClasses;
            this.newModulesRequired = newModulesRequired;
        }

        public boolean hasViolations() {
            return totalViolations > 0;
        }
    }

    static class ModuleViolation {
        private final String moduleId;
        private final String moduleName;
        private final List<OriginViolation> originViolations;

        public ModuleViolation(String moduleId, String moduleName, List<OriginViolation> originViolations) {
            this.moduleId = moduleId;
            this.moduleName = moduleName;
            this.originViolations = new ArrayList<>(originViolations);
        }
    }

    static class OriginViolation {
        private final String forbiddenOrigin;
        private final List<ViolatingClass> violatingClasses;
        private final boolean isCluster;

        public OriginViolation(String forbiddenOrigin, List<ViolatingClass> violatingClasses, boolean isCluster) {
            this.forbiddenOrigin = forbiddenOrigin;
            this.violatingClasses = new ArrayList<>(violatingClasses);
            this.isCluster = isCluster;
        }
    }

    static class ViolatingClass {
        private final String classId;
        private final String className;
        private final List<String> violatingDependencyFQNs;
        private final MoveSuggestion bestSuggestion;
        private final List<MoveSuggestion> alternativeSuggestions;

        public ViolatingClass(String classId, String className,
                            List<String> violatingDependencyFQNs,
                            MoveSuggestion bestSuggestion,
                            List<MoveSuggestion> alternativeSuggestions) {
            this.classId = classId;
            this.className = className;
            this.violatingDependencyFQNs = new ArrayList<>(violatingDependencyFQNs);
            this.bestSuggestion = bestSuggestion;
            this.alternativeSuggestions = new ArrayList<>(alternativeSuggestions);
        }
    }

    static class MoveSuggestion {
        private final String targetModuleId;
        private final String targetModuleName;
        private final double similarityImprovement;
        private final int newViolationsCreated;
        private final boolean requiresNewModule;
        private final String suggestionReason;

        public MoveSuggestion(String targetModuleId, String targetModuleName,
                            double similarityImprovement, int newViolationsCreated,
                            boolean requiresNewModule, String suggestionReason) {
            this.targetModuleId = targetModuleId;
            this.targetModuleName = targetModuleName;
            this.similarityImprovement = similarityImprovement;
            this.newViolationsCreated = newViolationsCreated;
            this.requiresNewModule = requiresNewModule;
            this.suggestionReason = suggestionReason;
        }
    }

    static class ViolationCluster {
        private final String forbiddenOrigin;
        private final String affectedModuleId;
        private final int classCount;
        private final String recommendation;

        public ViolationCluster(String forbiddenOrigin, String affectedModuleId,
                              int classCount, String recommendation) {
            this.forbiddenOrigin = forbiddenOrigin;
            this.affectedModuleId = affectedModuleId;
            this.classCount = classCount;
            this.recommendation = recommendation;
        }
    }

    public void architectureViolationAnalysis(List<Module> modules) {
        log.info("Starting Architecture Violation Analysis for {} modules...", modules.size());

        // TODO: Implement the analysis logic
        ArchitectureViolationResult result = analyzeViolations(modules);

        if (!result.hasViolations()) {
            log.info("Architecture Violation Analysis completed successfully. No violations found across {} modules.", modules.size());
            return;
        }

        logResults(result, modules.size());
    }

    private ArchitectureViolationResult analyzeViolations(List<Module> modules) {
        List<ModuleViolation> moduleViolations = new ArrayList<>();
        List<ViolationCluster> clusters = new ArrayList<>();
        int totalViolations = 0;
        int moveableClasses = 0;
        int newModulesRequired = 0;

        for (Module module : modules) {
            ModuleViolationAnalysis moduleAnalysis = analyzeModuleViolations(module, modules);

            if (moduleAnalysis.hasViolations()) {
                moduleViolations.add(moduleAnalysis.moduleViolation);
                clusters.addAll(moduleAnalysis.clusters);
                totalViolations += moduleAnalysis.violationCount;
                moveableClasses += moduleAnalysis.moveableClassCount;
                newModulesRequired += moduleAnalysis.newModulesRequiredCount;
            }
        }

        return new ArchitectureViolationResult(moduleViolations, clusters, totalViolations, moveableClasses, newModulesRequired);
    }

    private static class ModuleViolationAnalysis {
        final ModuleViolation moduleViolation;
        final List<ViolationCluster> clusters;
        final int violationCount;
        final int moveableClassCount;
        final int newModulesRequiredCount;

        public ModuleViolationAnalysis(ModuleViolation moduleViolation, List<ViolationCluster> clusters,
                                     int violationCount, int moveableClassCount, int newModulesRequiredCount) {
            this.moduleViolation = moduleViolation;
            this.clusters = clusters;
            this.violationCount = violationCount;
            this.moveableClassCount = moveableClassCount;
            this.newModulesRequiredCount = newModulesRequiredCount;
        }

        public boolean hasViolations() {
            return violationCount > 0;
        }
    }

    private ModuleViolationAnalysis analyzeModuleViolations(Module module, List<Module> allModules) {
        // Handle empty or null modules gracefully
        if (module == null || module.getClazzes() == null || module.getClazzes().isEmpty()) {
            return new ModuleViolationAnalysis(null, new ArrayList<>(), 0, 0, 0);
        }

        // Step 1: Extract allowed origins from allowed rules
        Set<String> allowedOrigins = (module.getAllowedRules() != null) ?
                module.getAllowedRules().stream()
                        .filter(rule -> rule != null && rule.getFullyQualifiedName() != null)
                        .map(AllowedRule::getFullyQualifiedName)
                        .collect(Collectors.toSet()) :
                new HashSet<>();

        // Step 2: Extract actual origins from dependencies
        Set<String> actualOrigins = (module.getAllDependenciesOrigin() != null) ?
                module.getAllDependenciesOrigin().stream()
                        .filter(origin -> origin != null && origin.getOriginName() != null)
                        .map(DependencyOrigin::getOriginName)
                        .collect(Collectors.toSet()) :
                new HashSet<>();

        // Skip modules with no dependencies
        if (actualOrigins.isEmpty()) {
            return new ModuleViolationAnalysis(null, new ArrayList<>(), 0, 0, 0);
        }

        // Step 3: Filter out self-dependencies (module depending on itself)
        String moduleOriginName = module.getName();
        String moduleId = module.getId();
        actualOrigins.remove(moduleOriginName);
        actualOrigins.remove(moduleId);

        // Step 4: Find violations - actual origins not in allowed origins
        Set<String> violatingOrigins = actualOrigins.stream()
                .filter(origin -> !allowedOrigins.contains(origin))
                .collect(Collectors.toSet());

        if (violatingOrigins.isEmpty()) {
            return new ModuleViolationAnalysis(null, new ArrayList<>(), 0, 0, 0);
        }

        // Step 5: For each violating origin, find the classes that depend on it
        List<OriginViolation> originViolations = new ArrayList<>();
        List<ViolationCluster> clusters = new ArrayList<>();
        int totalViolations = 0;
        int moveableClasses = 0;
        int newModulesRequired = 0;

        for (String forbiddenOrigin : violatingOrigins) {
            OriginViolationAnalysis originAnalysis = analyzeOriginViolation(module, forbiddenOrigin, allModules);
            originViolations.add(originAnalysis.originViolation);

            if (originAnalysis.isCluster) {
                clusters.add(new ViolationCluster(
                    forbiddenOrigin,
                    module.getId(),
                    originAnalysis.classCount,
                    "Consider changing module reference class"
                ));
            }

            totalViolations += originAnalysis.classCount;
            moveableClasses += originAnalysis.moveableClassCount;
            newModulesRequired += originAnalysis.newModulesRequiredCount;
        }

        ModuleViolation moduleViolation = new ModuleViolation(module.getId(), module.getName(), originViolations);

        return new ModuleViolationAnalysis(moduleViolation, clusters, totalViolations, moveableClasses, newModulesRequired);
    }

    private static class OriginViolationAnalysis {
        final OriginViolation originViolation;
        final boolean isCluster;
        final int classCount;
        final int moveableClassCount;
        final int newModulesRequiredCount;

        public OriginViolationAnalysis(OriginViolation originViolation, boolean isCluster, int classCount,
                                     int moveableClassCount, int newModulesRequiredCount) {
            this.originViolation = originViolation;
            this.isCluster = isCluster;
            this.classCount = classCount;
            this.moveableClassCount = moveableClassCount;
            this.newModulesRequiredCount = newModulesRequiredCount;
        }
    }

    private OriginViolationAnalysis analyzeOriginViolation(Module module, String forbiddenOrigin, List<Module> allModules) {
        List<ViolatingClass> violatingClasses = new ArrayList<>();

        // Find all classes in the module that depend on the forbidden origin
        for (Clazz clazz : module.getClazzes()) {
            List<String> violatingDependencyFQNs = findViolatingDependencies(clazz, forbiddenOrigin);

            if (!violatingDependencyFQNs.isEmpty()) {
                // Generate move suggestions for this violating class
                MoveSuggestionAnalysis suggestionAnalysis = generateMoveSuggestions(clazz, module, forbiddenOrigin, allModules);

                ViolatingClass violatingClass = new ViolatingClass(
                    clazz.getId(),
                    clazz.getName(),
                    violatingDependencyFQNs,
                    suggestionAnalysis.bestSuggestion,
                    suggestionAnalysis.alternativeSuggestions
                );

                violatingClasses.add(violatingClass);
            }
        }

        // Determine if this is a cluster (3+ classes with same violation)
        boolean isCluster = violatingClasses.size() >= 3;

        // Count moveable classes and new modules required
        int moveableClasses = 0;
        int newModulesRequired = 0;

        for (ViolatingClass violatingClass : violatingClasses) {
            if (violatingClass.bestSuggestion != null) {
                if (violatingClass.bestSuggestion.requiresNewModule) {
                    newModulesRequired++;
                } else {
                    moveableClasses++;
                }
            }
        }

        OriginViolation originViolation = new OriginViolation(forbiddenOrigin, violatingClasses, isCluster);

        return new OriginViolationAnalysis(
            originViolation,
            isCluster,
            violatingClasses.size(),
            moveableClasses,
            newModulesRequired
        );
    }

    private List<String> findViolatingDependencies(Clazz clazz, String forbiddenOrigin) {
        List<String> violatingFQNs = new ArrayList<>();

        // Handle null or empty dependencies gracefully
        if (clazz == null || clazz.getDependencies() == null || forbiddenOrigin == null) {
            return violatingFQNs;
        }

        for (Dependency dependency : clazz.getDependencies()) {
            if (dependency != null &&
                dependency.getPackageName() != null &&
                dependency.getPackageName().equals(forbiddenOrigin)) {
                // Collect all types from this forbidden package
                if (dependency.getTypes() != null) {
                    dependency.getTypes().forEach(type -> violatingFQNs.add(type.getFullyQualifiedName()));
                }
            }
        }

        return violatingFQNs;
    }

    private static class MoveSuggestionAnalysis {
        final MoveSuggestion bestSuggestion;
        final List<MoveSuggestion> alternativeSuggestions;

        public MoveSuggestionAnalysis(MoveSuggestion bestSuggestion, List<MoveSuggestion> alternativeSuggestions) {
            this.bestSuggestion = bestSuggestion;
            this.alternativeSuggestions = new ArrayList<>(alternativeSuggestions);
        }
    }

    private MoveSuggestionAnalysis generateMoveSuggestions(Clazz clazz, Module sourceModule,
                                                          String forbiddenOrigin, List<Module> allModules) {
        List<MoveSuggestion> suggestions = new ArrayList<>();

        // Handle null inputs gracefully
        if (clazz == null || sourceModule == null || forbiddenOrigin == null || allModules == null) {
            MoveSuggestion noTargetSuggestion = new MoveSuggestion(
                null, "New Module", 0.0, 0, true, "No existing module accepts this dependency"
            );
            return new MoveSuggestionAnalysis(noTargetSuggestion, new ArrayList<>());
        }

        // Search all modules where forbidden origins are in allowedRules
        for (Module targetModule : allModules) {
            if (targetModule == null || targetModule.getId() == null ||
                targetModule.getId().equals(sourceModule.getId())) {
                continue; // Skip null or same module
            }

            // Check if this target module allows the forbidden origin
            boolean allowsForbiddenOrigin = (targetModule.getAllowedRules() != null) &&
                targetModule.getAllowedRules().stream()
                    .filter(rule -> rule != null && rule.getFullyQualifiedName() != null)
                    .anyMatch(rule -> rule.getFullyQualifiedName().equals(forbiddenOrigin));

            if (allowsForbiddenOrigin) {
                // Calculate similarity improvement and new violations
                SimilarityCalculation calculation = calculateMoveImpact(clazz, sourceModule, targetModule);

                String reason = determineMoveSuggestionReason(calculation);

                MoveSuggestion suggestion = new MoveSuggestion(
                    targetModule.getId(),
                    targetModule.getName(),
                    calculation.similarityImprovement,
                    calculation.newViolationsCreated,
                    false, // Not requiring new module
                    reason
                );

                suggestions.add(suggestion);
            }
        }

        // Sort suggestions by criteria: similarity improvement > violation count > module size
        suggestions.sort((a, b) -> {
            // First criterion: similarity improvement (higher is better)
            int similarityCompare = Double.compare(b.similarityImprovement, a.similarityImprovement);
            if (similarityCompare != 0) return similarityCompare;

            // Second criterion: new violations created (lower is better)
            int violationCompare = Integer.compare(a.newViolationsCreated, b.newViolationsCreated);
            if (violationCompare != 0) return violationCompare;

            // Third criterion could be module size, but we don't have that info readily available
            return 0;
        });

        MoveSuggestion bestSuggestion = null;
        List<MoveSuggestion> alternatives = new ArrayList<>();

        if (!suggestions.isEmpty()) {
            bestSuggestion = suggestions.get(0);
            alternatives = suggestions.subList(1, Math.min(suggestions.size(), 4)); // Max 3 alternatives
        } else {
            // No valid targets found, suggest creating new module
            bestSuggestion = new MoveSuggestion(
                null,
                "New Module",
                0.0,
                0,
                true,
                "No existing module accepts this dependency"
            );
        }

        return new MoveSuggestionAnalysis(bestSuggestion, alternatives);
    }

    private static class SimilarityCalculation {
        final double similarityImprovement;
        final int newViolationsCreated;

        public SimilarityCalculation(double similarityImprovement, int newViolationsCreated) {
            this.similarityImprovement = similarityImprovement;
            this.newViolationsCreated = newViolationsCreated;
        }
    }

    private SimilarityCalculation calculateMoveImpact(Clazz clazz, Module sourceModule, Module targetModule) {
        // Create hypothetical modules to test the move impact
        Module sourceWithoutClass = createModuleWithRemovedClass(sourceModule, clazz);
        Module targetWithClass = createModuleWithAddedClass(targetModule, clazz);

        // Calculate similarity improvements
        double originalSourceSimilarity = sourceModule.getSimilarity();
        double originalTargetSimilarity = targetModule.getSimilarity();

        this.moduleService.calculateClassSimilarities(sourceWithoutClass);
        this.moduleService.calculateModuleSimilarity(sourceWithoutClass);
        this.moduleService.calculateClassSimilarities(targetWithClass);
        this.moduleService.calculateModuleSimilarity(targetWithClass);

        double newSourceSimilarity = sourceWithoutClass.getSimilarity();
        double newTargetSimilarity = targetWithClass.getSimilarity();

        double sourceImprovement = newSourceSimilarity - originalSourceSimilarity;
        double targetImprovement = newTargetSimilarity - originalTargetSimilarity;
        double totalImprovement = sourceImprovement + targetImprovement;

        // Calculate new violations that would be created (simplified - would need full analysis)
        int newViolationsCreated = estimateNewViolations(clazz, targetModule);

        return new SimilarityCalculation(totalImprovement, newViolationsCreated);
    }

    private Module createModuleWithRemovedClass(Module module, Clazz classToRemove) {
        List<Clazz> newClazzes = module.getClazzes().stream()
                .filter(clazz -> !clazz.getId().equals(classToRemove.getId()))
                .collect(Collectors.toList());

        return new Module(
                module.getId() + "_temp_without_" + classToRemove.getId(),
                module.getName(),
                module.getRefClazzes(),
                module.getAllowedRules(),
                module.getAllDependenciesOrigin(),
                module.getRefClazzesDependencies(),
                module.getAllDependencies(),
                newClazzes,
                0.0
        );
    }

    private Module createModuleWithAddedClass(Module module, Clazz classToAdd) {
        List<Clazz> newClazzes = new ArrayList<>(module.getClazzes());
        newClazzes.add(classToAdd);

        return new Module(
                module.getId() + "_temp_with_" + classToAdd.getId(),
                module.getName(),
                module.getRefClazzes(),
                module.getAllowedRules(),
                module.getAllDependenciesOrigin(),
                module.getRefClazzesDependencies(),
                module.getAllDependencies(),
                newClazzes,
                0.0
        );
    }

    private int estimateNewViolations(Clazz clazz, Module targetModule) {
        // Simplified estimation - in a full implementation, this would perform
        // a complete violation analysis on the hypothetical target module
        if (clazz.getDependencies() == null) return 0;

        Set<String> targetAllowedOrigins = targetModule.getAllowedRules().stream()
                .map(AllowedRule::getFullyQualifiedName)
                .collect(Collectors.toSet());

        int newViolations = 0;
        for (Dependency dependency : clazz.getDependencies()) {
            if (dependency.getPackageName() != null &&
                !targetAllowedOrigins.contains(dependency.getPackageName())) {
                newViolations++;
            }
        }

        return newViolations;
    }

    private String determineMoveSuggestionReason(SimilarityCalculation calculation) {
        if (calculation.similarityImprovement > 0.30) {
            return "Perfect architectural fit";
        } else if (calculation.similarityImprovement > 0.20) {
            return "Highest similarity improvement";
        } else if (calculation.newViolationsCreated == 0) {
            return "No new violations created";
        } else if (calculation.newViolationsCreated <= 2) {
            return "Minimal violation impact";
        } else {
            return "Acceptable minimum violations";
        }
    }

    private void logResults(ArchitectureViolationResult result, int totalModules) {
        StringBuilder output = new StringBuilder();

        output.append("\n=== Architecture Violation Analysis Results ===\n\n");
        output.append("✅ Analysis completed successfully across ").append(totalModules).append(" modules\n\n");

        // Summary section
        output.append("📊 Summary: ").append(result.totalViolations).append(" violations found affecting ")
               .append(result.moduleViolations.size()).append(" modules\n");
        output.append("├── ").append(result.moveableClasses).append(" classes require relocation\n");
        output.append("├── ").append(result.newModulesRequired).append(" new modules suggested\n");
        output.append("└── ").append(result.clusters.size()).append(" violation clusters detected\n\n");

        // Module violations section
        if (!result.moduleViolations.isEmpty()) {
            output.append("🚨 Module Violations:\n\n");

            for (int i = 0; i < result.moduleViolations.size(); i++) {
                ModuleViolation moduleViolation = result.moduleViolations.get(i);
                boolean isLastModule = (i == result.moduleViolations.size() - 1);

                output.append("Module: ").append(moduleViolation.moduleName)
                       .append(" (ID: ").append(moduleViolation.moduleId).append(")\n");

                for (int j = 0; j < moduleViolation.originViolations.size(); j++) {
                    OriginViolation originViolation = moduleViolation.originViolations.get(j);
                    boolean isLastOrigin = (j == moduleViolation.originViolations.size() - 1);

                    String originPrefix = isLastModule && isLastOrigin ? "└── " : "├── ";
                    output.append(originPrefix).append("🔴 Forbidden Origin: ").append(originViolation.forbiddenOrigin).append("\n");

                    // Check if this is a cluster
                    if (originViolation.isCluster) {
                        String clusterPrefix = isLastModule && isLastOrigin ? "│   ├── " : "│   ├── ";
                        output.append(clusterPrefix).append("Violation Cluster: ").append(originViolation.violatingClasses.size()).append(" classes affected\n");
                        output.append(clusterPrefix.replace("├──", "│   ")).append("💡 Recommendation: Consider changing module reference class\n");
                    }

                    // List violating classes
                    for (int k = 0; k < originViolation.violatingClasses.size(); k++) {
                        ViolatingClass violatingClass = originViolation.violatingClasses.get(k);
                        boolean isLastClass = (k == originViolation.violatingClasses.size() - 1);

                        String classPrefix = isLastModule && isLastOrigin && isLastClass ? "│   └── " : "│   ├── ";
                        output.append(classPrefix).append("Violating Class: ").append(violatingClass.className).append("\n");

                        // Dependencies
                        String depPrefix = isLastModule && isLastOrigin && isLastClass ? "│   │   ├── " : "│   │   ├── ";
                        output.append(depPrefix).append("Dependencies: ").append(String.join(", ", violatingClass.violatingDependencyFQNs)).append("\n");

                        // Best suggestion
                        if (violatingClass.bestSuggestion != null) {
                            String suggPrefix = isLastModule && isLastOrigin && isLastClass ? "│   │   └── " : "│   │   └── ";

                            if (violatingClass.bestSuggestion.requiresNewModule) {
                                output.append(suggPrefix).append("🆕 Suggestion: Create new module\n");
                                String reasonPrefix = isLastModule && isLastOrigin && isLastClass ? "│   │       └── " : "│   │       └── ";
                                output.append(reasonPrefix).append("Reason: ").append(violatingClass.bestSuggestion.suggestionReason).append("\n");
                            } else {
                                output.append(suggPrefix).append("🎯 Best Move: ").append(violatingClass.bestSuggestion.targetModuleName).append("\n");
                                String reasonPrefix = isLastModule && isLastOrigin && isLastClass ? "│   │       ├── " : "│   │       ├── ";
                                output.append(reasonPrefix).append("Similarity improvement: ")
                                       .append(String.format("%+.2f", violatingClass.bestSuggestion.similarityImprovement))
                                       .append("\n");
                                output.append(reasonPrefix).append("New violations: ").append(violatingClass.bestSuggestion.newViolationsCreated).append("\n");
                                String finalPrefix = isLastModule && isLastOrigin && isLastClass ? "│   │       └── " : "│   │       └── ";
                                output.append(finalPrefix).append("Reason: ").append(violatingClass.bestSuggestion.suggestionReason).append("\n");
                            }
                        }
                    }
                }

                if (!isLastModule) {
                    output.append("\n");
                }
            }
        }

        // Move recommendations summary
        output.append("\n🎯 Move Recommendations Summary:\n");
        long highConfidenceMoves = result.moduleViolations.stream()
                .flatMap(mv -> mv.originViolations.stream())
                .flatMap(ov -> ov.violatingClasses.stream())
                .filter(vc -> vc.bestSuggestion != null && !vc.bestSuggestion.requiresNewModule && vc.bestSuggestion.similarityImprovement > 0.20)
                .count();

        double avgImprovement = result.moduleViolations.stream()
                .flatMap(mv -> mv.originViolations.stream())
                .flatMap(ov -> ov.violatingClasses.stream())
                .filter(vc -> vc.bestSuggestion != null && !vc.bestSuggestion.requiresNewModule)
                .mapToDouble(vc -> vc.bestSuggestion.similarityImprovement)
                .average()
                .orElse(0.0);

        output.append("├── ").append(result.moveableClasses).append(" classes can move to existing modules")
               .append(" (avg similarity improvement: ").append(String.format("%+.2f", avgImprovement)).append(")\n");
        output.append("├── ").append(result.newModulesRequired).append(" classes require new module creation\n");
        output.append("└── ").append(result.clusters.size()).append(" violation clusters need reference class review\n\n");

        // Next steps
        output.append("💡 Next Steps:\n");
        output.append("1. Review violation clusters for potential reference class changes\n");
        output.append("2. Execute high-confidence moves (similarity improvement > 0.20)\n");
        output.append("3. Consider creating new modules for orphaned classes\n");
        output.append("4. Re-run analysis after changes to verify improvements\n");

        log.info(output.toString());
    }
}