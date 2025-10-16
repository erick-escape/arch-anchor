package tcc.com.viewer.services.architecturalAnalyses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.services.ModuleService;

import java.util.ArrayList;
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
        private final List<Violation> violations;

        public ModuleViolation(String moduleId, String moduleName, List<Violation> violations) {
            this.moduleId = moduleId;
            this.moduleName = moduleName;
            this.violations = new ArrayList<>(violations);
        }
    }

    static class Violation {
        private final String violation;
        private final List<ViolatingClass> violatingClasses;
        private final boolean isCluster;

        public Violation(String violation, List<ViolatingClass> violatingClasses, boolean isCluster) {
            this.violation = violation;
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
        private final String violation;
        private final String affectedModuleId;
        private final int classCount;
        private final String recommendation;

        public ViolationCluster(String violation, String affectedModuleId,
                                int classCount, String recommendation) {
            this.violation = violation;
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

        // Step 1: Extract allowed rules from refClazzesDependencies
        Set<String> allowedRules = (module.getRefClazzesDependencies() != null) ?
                module.getRefClazzesDependencies().stream()
                        .filter(dep -> dep != null && dep.getPackageName() != null)
                        .map(Dependency::getPackageName)
                        .collect(Collectors.toSet()) :
                new HashSet<>();

        // Step 2: Extract dependencies used by classes from moduleDependencies
        Set<String> moduleDependencies = (module.getModuleDependencies() != null) ?
                module.getModuleDependencies().stream()
                        .filter(dep -> dep != null && dep.getPackageName() != null)
                        .map(Dependency::getPackageName)
                        .collect(Collectors.toSet()) :
                new HashSet<>();

        // Skip modules with no dependencies
        if (moduleDependencies.isEmpty()) {
            return new ModuleViolationAnalysis(null, new ArrayList<>(), 0, 0, 0);
        }

        // Step 3: Filter out self-dependencies (module depending on itself)
        String moduleName = module.getName();
        String moduleId = module.getId();
        moduleDependencies.remove(moduleName);
        moduleDependencies.remove(moduleId);

        // Step 4: Find violations - dependencies not in allowed rules
        Set<String> violations = moduleDependencies.stream()
                .filter(dep -> !allowedRules.contains(dep))
                .collect(Collectors.toSet());

        if (violations.isEmpty()) {
            return new ModuleViolationAnalysis(null, new ArrayList<>(), 0, 0, 0);
        }

        // Step 5: For each violation, find the classes that depend on it
        List<Violation> violationClazz = new ArrayList<>();
        List<ViolationCluster> clusters = new ArrayList<>();
        int totalViolations = 0;
        int moveableClasses = 0;
        int newModulesRequired = 0;

        for (String violation : violations) {
            ViolationAnalysis violationAnalysis = analyzeViolation(module, violation, allModules);
            violationClazz.add(violationAnalysis.violation);

            if (violationAnalysis.isCluster) {
                clusters.add(new ViolationCluster(
                        violation,
                        module.getId(),
                        violationAnalysis.classCount,
                        "Consider changing module reference class"
                ));
            }

            totalViolations += violationAnalysis.classCount;
            moveableClasses += violationAnalysis.moveableClassCount;
            newModulesRequired += violationAnalysis.newModulesRequiredCount;
        }

        ModuleViolation moduleViolation = new ModuleViolation(module.getId(), module.getName(), violationClazz);

        return new ModuleViolationAnalysis(moduleViolation, clusters, totalViolations, moveableClasses, newModulesRequired);
    }

    private static class ViolationAnalysis {
        final Violation violation;
        final boolean isCluster;
        final int classCount;
        final int moveableClassCount;
        final int newModulesRequiredCount;

        public ViolationAnalysis(Violation violation, boolean isCluster, int classCount,
                                 int moveableClassCount, int newModulesRequiredCount) {
            this.violation = violation;
            this.isCluster = isCluster;
            this.classCount = classCount;
            this.moveableClassCount = moveableClassCount;
            this.newModulesRequiredCount = newModulesRequiredCount;
        }
    }

    private ViolationAnalysis analyzeViolation(Module module, String violation, List<Module> allModules) {
        List<ViolatingClass> violatingClasses = new ArrayList<>();

        // Find all classes in the module that depend on the violation
        for (Clazz clazz : module.getClazzes()) {
            List<String> violatingDependencyFQNs = checkIfClassHasViolation(clazz, violation);

            if (!violatingDependencyFQNs.isEmpty()) {
                // Generate move suggestions for this violating class
                MoveSuggestionAnalysis suggestionAnalysis = generateMoveSuggestions(clazz, module, violation, allModules);

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

        Violation originViolation = new Violation(violation, violatingClasses, isCluster);

        return new ViolationAnalysis(
                originViolation,
                isCluster,
                violatingClasses.size(),
                moveableClasses,
                newModulesRequired
        );
    }

    private List<String> checkIfClassHasViolation(Clazz clazz, String violation) {
        List<String> violatingFQNs = new ArrayList<>();

        // Handle null or empty dependencies gracefully
        if (clazz == null || clazz.getDependencies() == null || violation == null) {
            return violatingFQNs;
        }

        for (Dependency dependency : clazz.getDependencies()) {
            if (dependency != null &&
                    dependency.getPackageName() != null &&
                    dependency.getPackageName().equals(violation)) {
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
                                                           String violation, List<Module> allModules) {
        List<MoveSuggestion> suggestions = new ArrayList<>();

        // Handle null inputs gracefully
        if (clazz == null || sourceModule == null || violation == null || allModules == null) {
            MoveSuggestion noTargetSuggestion = new MoveSuggestion(
                    null, "New Module", 0.0, 0, true, "No existing module accepts this dependency"
            );
            return new MoveSuggestionAnalysis(noTargetSuggestion, new ArrayList<>());
        }

        // Search all modules where the violation is in the allowed rules
        for (Module targetModule : allModules) {
            if (targetModule == null || targetModule.getId() == null ||
                    targetModule.getId().equals(sourceModule.getId())) {
                continue; // Skip null or same module
            }

            // Check if this target module allows the forbidden origin
            boolean allowsViolation = (targetModule.getRefClazzesDependencies() != null) &&
                    targetModule.getRefClazzesDependencies().stream()
                            .filter(dep -> dep != null && dep.getPackageName() != null)
                            .anyMatch(dep -> dep.getPackageName().equals(violation));

            if (allowsViolation) {
                // Calculate similarity improvement and new violations
                SimilarityCalculation calculation = calculateMoveImpact(clazz, sourceModule, targetModule);
                if (calculation.newViolationsCreated > calculation.currentViolations ||
                        calculation.sourceImprovement < 0 || calculation.targetImprovement < 0) {
                    continue;
                }
                String reason = determineMoveSuggestionReason(calculation);

                MoveSuggestion suggestion = new MoveSuggestion(
                        targetModule.getId(),
                        targetModule.getName(),
                        calculation.sourceImprovement + calculation.targetImprovement,
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
        final double sourceImprovement;
        final double targetImprovement;
        final int newViolationsCreated;
        final int currentViolations;

        public SimilarityCalculation(double sourceImprovement, double targetImprovement, int newViolationsCreated, int currentViolations) {
            this.sourceImprovement = sourceImprovement;
            this.targetImprovement = targetImprovement;
            this.newViolationsCreated = newViolationsCreated;
            this.currentViolations = currentViolations;
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
        // Calculate new violations that would be created (simplified - would need full analysis)
        int newViolationsCreated = getClazzViolations(clazz, targetModule);
        int currentViolations = getClazzViolations(clazz, sourceModule);
        double sourceImprovement = newSourceSimilarity - originalSourceSimilarity;
        double targetImprovement = newTargetSimilarity - originalTargetSimilarity;
//        double totalImprovement = sourceImprovement + targetImprovement;

        return new SimilarityCalculation(sourceImprovement, targetImprovement, newViolationsCreated, currentViolations);
    }

    private Module createModuleWithRemovedClass(Module module, Clazz classToRemove) {
        List<Clazz> newClazzes = module.getClazzes().stream()
                .filter(clazz -> !clazz.getId().equals(classToRemove.getId()))
                .collect(Collectors.toList());

        return new Module(
                module.getId() + "_temp_without_" + classToRemove.getId(),
                module.getName(),
                module.getRefClazzes(),
                module.getRefClazzesDependencies(),
                module.getModuleDependencies(),
                newClazzes,
                0.0,
                0.0,
                0 // violations will be calculated later
        );
    }

    private Module createModuleWithAddedClass(Module module, Clazz classToAdd) {
        List<Clazz> newClazzes = new ArrayList<>(module.getClazzes());
        newClazzes.add(classToAdd);

        return new Module(
                module.getId() + "_temp_with_" + classToAdd.getId(),
                module.getName(),
                module.getRefClazzes(),
                module.getRefClazzesDependencies(),
                module.getModuleDependencies(),
                newClazzes,
                0.0,
                0.0,
                0 // violations will be calculated later
        );
    }

    private int getClazzViolations(Clazz clazz, Module module) {
        // Simplified estimation - in a full implementation, this would perform
        // a complete violation analysis on the hypothetical target module
        if (clazz.getDependencies() == null) return 0;

        Set<String> allowedRules = module.getRefClazzesDependencies().stream()
                .map(Dependency::getPackageName)
                .collect(Collectors.toSet());

        int violations = 0;
        for (Dependency dependency : clazz.getDependencies()) {
            if (dependency.getPackageName() != null &&
                    !allowedRules.contains(dependency.getPackageName())) {
                violations++;
            }
        }

        return violations;
    }

    private String determineMoveSuggestionReason(SimilarityCalculation calculation) {
        if (calculation.sourceImprovement > 0 && calculation.targetImprovement > 0) {
            if (calculation.newViolationsCreated == 0)
                return "Perfect architectural fit, improved similarity and no new violations";
            if (calculation.newViolationsCreated < calculation.currentViolations)
                return "Improved similarity and decreased violations";
        } else if (calculation.sourceImprovement == 0 && calculation.targetImprovement > 0) {
            if (calculation.newViolationsCreated == 0)
                return "Target similarity improved and no new violations created";
            if (calculation.newViolationsCreated < calculation.currentViolations)
                return "Target similarity improved and decreased violations";
        } else if (calculation.sourceImprovement > 0 && calculation.targetImprovement == 0) {
            if (calculation.newViolationsCreated == 0)
                return "Source similarity improved and no new violations created";
            if (calculation.newViolationsCreated < calculation.currentViolations)
                return "Source similarity improved and decreased violations";
        } else if (calculation.sourceImprovement == 0 && calculation.targetImprovement == 0) {
            if (calculation.newViolationsCreated == 0) return "No similarity improvement but no new violations created";
        }

        return "This should not be a recommendation";
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

                for (int j = 0; j < moduleViolation.violations.size(); j++) {
                    Violation violation = moduleViolation.violations.get(j);
                    boolean isLastOrigin = (j == moduleViolation.violations.size() - 1);

                    String originPrefix = isLastModule && isLastOrigin ? "└── " : "├── ";
                    output.append(originPrefix).append("🔴 Forbidden Origin: ").append(violation.violation).append("\n");

                    // Check if this is a cluster
                    if (violation.isCluster) {
                        String clusterPrefix = isLastModule && isLastOrigin ? "│   ├── " : "│   ├── ";
                        output.append(clusterPrefix).append("Violation Cluster: ").append(violation.violatingClasses.size()).append(" classes affected\n");
                        output.append(clusterPrefix.replace("├──", "│   ")).append("💡 Recommendation: Consider changing module reference class\n");
                    }

                    // List violating classes
                    for (int k = 0; k < violation.violatingClasses.size(); k++) {
                        ViolatingClass violatingClass = violation.violatingClasses.get(k);
                        boolean isLastClass = (k == violation.violatingClasses.size() - 1);

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
                .flatMap(mv -> mv.violations.stream())
                .flatMap(ov -> ov.violatingClasses.stream())
                .filter(vc -> vc.bestSuggestion != null && !vc.bestSuggestion.requiresNewModule && vc.bestSuggestion.similarityImprovement > 0.20)
                .count();

        double avgImprovement = result.moduleViolations.stream()
                .flatMap(mv -> mv.violations.stream())
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