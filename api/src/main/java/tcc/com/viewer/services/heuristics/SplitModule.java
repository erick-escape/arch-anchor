package tcc.com.viewer.services.heuristics;

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
public class SplitModule extends Heuristic {
    public SplitModule(ModuleService moduleService) {
        super(moduleService);
    }

    @Override
    public void execute(List<Module> modules) {
        splitModuleHeuristic(modules);
    }

    private static class SplitModuleResult {
        String originalModuleId;
        String originalModuleName;
        String remainingModuleId;
        String remainingModuleName;
        String newModuleId;
        String newModuleName;
        List<String> splitClassNames;
        double originalSimilarity;
        double remainingSimilarity;
        double newModuleSimilarity;
        double totalImprovement;

        public SplitModuleResult(String originalModuleId, String originalModuleName,
                                 String remainingModuleId, String remainingModuleName,
                                 String newModuleId, String newModuleName,
                                 List<String> splitClassNames,
                                 double originalSimilarity, double remainingSimilarity,
                                 double newModuleSimilarity, double totalImprovement) {
            this.originalModuleId = originalModuleId;
            this.originalModuleName = originalModuleName;
            this.remainingModuleId = remainingModuleId;
            this.remainingModuleName = remainingModuleName;
            this.newModuleId = newModuleId;
            this.newModuleName = newModuleName;
            this.splitClassNames = new ArrayList<>(splitClassNames);
            this.originalSimilarity = originalSimilarity;
            this.remainingSimilarity = remainingSimilarity;
            this.newModuleSimilarity = newModuleSimilarity;
            this.totalImprovement = totalImprovement;
        }
    }

    private Module createModuleWithClasses(Module originalModule, List<Clazz> classes, String suffix) {
        String newId = originalModule.getId() + "_" + suffix;
        String newName = originalModule.getName() + " (" + suffix + ")";

        return new Module(
                newId,
                newName,
                originalModule.getRefClazzes(),
                originalModule.getAllowedRules(),
                new ArrayList<>(classes),
                0.0
        );
    }

    private Module createModuleWithoutClasses(Module originalModule, List<Clazz> classesToRemove) {
        List<Clazz> remainingClasses = originalModule.getClazzes().stream()
                .filter(clazz -> classesToRemove.stream()
                        .noneMatch(toRemove -> toRemove.getId().equals(clazz.getId())))
                .collect(Collectors.toList());

        String newId = originalModule.getId() + "_remaining";
        String newName = originalModule.getName() + " (remaining)";

        return new Module(
                newId,
                newName,
                originalModule.getRefClazzes(),
                originalModule.getAllowedRules(),
                remainingClasses,
                0.0
        );
    }

    private void generateClassCombinations(List<Clazz> classes, int combinationSize, int start,
                                           List<Clazz> currentCombination, List<List<Clazz>> allCombinations) {
        if (currentCombination.size() == combinationSize) {
            allCombinations.add(new ArrayList<>(currentCombination));
            return;
        }

        for (int i = start; i < classes.size(); i++) {
            currentCombination.add(classes.get(i));
            generateClassCombinations(classes, combinationSize, i + 1, currentCombination, allCombinations);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }

    private List<List<Clazz>> getAllClassCombinations(List<Clazz> classes, int combinationSize) {
        List<List<Clazz>> allCombinations = new ArrayList<>();
        generateClassCombinations(classes, combinationSize, 0, new ArrayList<>(), allCombinations);
        return allCombinations;
    }

    private boolean isSplitBeneficial(Module original, Module remaining, Module newModule) {
        this.moduleService.calculateClassSimilarities(remaining);
        this.moduleService.calculateModuleSimilarity(remaining);
        this.moduleService.calculateClassSimilarities(newModule);
        this.moduleService.calculateModuleSimilarity(newModule);

        double originalSimilarity = original.getSimilarity();
        double remainingSimilarity = remaining.getSimilarity();
        double newModuleSimilarity = newModule.getSimilarity();

        return remainingSimilarity > originalSimilarity && newModuleSimilarity > originalSimilarity;
    }

    private double calculateTotalImprovement(Module original, Module remaining, Module newModule) {
        double originalSimilarity = original.getSimilarity();
        double remainingSimilarity = remaining.getSimilarity();
        double newModuleSimilarity = newModule.getSimilarity();

        double remainingImprovement = remainingSimilarity - originalSimilarity;
        double newModuleImprovement = newModuleSimilarity - originalSimilarity;

        return remainingImprovement + newModuleImprovement;
    }

    public void splitModuleHeuristic(List<Module> modules) {
        log.info("Starting split module heuristic analysis for {} modules...", modules.size());

        List<SplitModuleResult> potentialSplits = new ArrayList<>();

        for (Module module : modules) {
            List<Clazz> classes = module.getClazzes();

            if (classes.size() < 2) {
                log.debug("Skipping module '{}' - insufficient classes for splitting ({})", module.getName(), classes.size());
                continue;
            }

            log.info("Analyzing splits for module '{}' with {} classes...", module.getName(), classes.size());

            // Try all possible combinations from size 1 to (total classes - 1)
            for (int splitSize = 1; splitSize < classes.size(); splitSize++) {
                List<List<Clazz>> combinations = getAllClassCombinations(classes, splitSize);

                for (List<Clazz> classesToSplit : combinations) {
                    Module remainingModule = createModuleWithoutClasses(module, classesToSplit);
                    Module newModule = createModuleWithClasses(module, classesToSplit, "split");

                    if (isSplitBeneficial(module, remainingModule, newModule)) {
                        List<String> splitClassNames = classesToSplit.stream()
                                .map(Clazz::getName)
                                .collect(Collectors.toList());

                        double totalImprovement = calculateTotalImprovement(module, remainingModule, newModule);

                        potentialSplits.add(new SplitModuleResult(
                                module.getId(),
                                module.getName(),
                                remainingModule.getId(),
                                remainingModule.getName(),
                                newModule.getId(),
                                newModule.getName(),
                                splitClassNames,
                                module.getSimilarity(),
                                remainingModule.getSimilarity(),
                                newModule.getSimilarity(),
                                totalImprovement
                        ));
                    }
                }
            }
        }

        potentialSplits.sort((a, b) -> Double.compare(b.totalImprovement, a.totalImprovement));

        log.info("Found {} potential beneficial splits", potentialSplits.size());
        for (int i = 0; i < Math.min(10, potentialSplits.size()); i++) {
            SplitModuleResult split = potentialSplits.get(i);
            log.info("Split suggestion: Module '{}' (sim: '{}') -> Remaining (sim: '{}') + New '{}' (sim: '{}') - Classes: {} -" +
                            "Total Improvement: '{}'",
                    split.originalModuleName,
                    String.format("%.3f", split.originalSimilarity),
                    String.format("%.3f", split.remainingSimilarity),
                    split.newModuleName,
                    String.format("%.3f", split.newModuleSimilarity),
                    String.join(", ", split.splitClassNames),
                    String.format("%.3f", split.totalImprovement)
            );
        }
    }
}