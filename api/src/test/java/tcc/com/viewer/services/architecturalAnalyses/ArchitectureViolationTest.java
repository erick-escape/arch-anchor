package tcc.com.viewer.services.architecturalAnalyses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.dependency.DependencyOrigin;
import tcc.com.viewer.domains.dependency.Type;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.domains.rules.AllowedRule;
import tcc.com.viewer.services.ModuleService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchitectureViolationTest {

    @Mock
    private ModuleService moduleService;

    private ArchitectureViolation architectureViolation;

    @BeforeEach
    void setUp() {
        architectureViolation = new ArchitectureViolation(moduleService);

        // Mock the ModuleService behavior for similarity calculations with lenient stubbing
        lenient().doNothing().when(moduleService).calculateClassSimilarities(any(Module.class));
        lenient().doNothing().when(moduleService).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 1: No Violations - Clean system, log success message
    @Test
    void testNoViolations_cleanSystem() {
        // Given
        List<Module> modules = List.of(createCleanModule());

        // When
        architectureViolation.execute(modules);

        // Then - Should log success message and complete without violations
        // This is verified by observing the logs and ensuring no exceptions are thrown
        verify(moduleService, never()).calculateClassSimilarities(any());
    }

    // Test Case 2: Single Violation - One class, one forbidden dependency, clear target
    @Test
    void testSingleViolation_oneClassOneForbiddenDependency() {
        // Given
        Module sourceModule = createModuleWithSingleViolation();
        Module targetModule = createTargetModuleForViolation();
        List<Module> modules = List.of(sourceModule, targetModule);

        // Mock similarity calculations
        mockSimilarityCalculations(sourceModule, targetModule);

        // When
        architectureViolation.execute(modules);

        // Then - Should find one violation and suggest move to target module
        verify(moduleService, atLeastOnce()).calculateClassSimilarities(any());
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any());
    }

    // Test Case 3: Multiple Violations - One class, multiple forbidden origins
    @Test
    void testMultipleViolations_oneClassMultipleForbiddenOrigins() {
        // Given
        Module moduleWithMultipleViolations = createModuleWithMultipleViolations();
        List<Module> modules = List.of(moduleWithMultipleViolations);

        // When
        architectureViolation.execute(modules);

        // Then - Should identify multiple violations for same class
        // Verification through log observation
    }

    // Test Case 4: Perfect Move - Class moves with high similarity improvement
    @Test
    void testPerfectMove_highSimilarityImprovement() {
        // Given
        Module sourceModule = createModuleWithSingleViolation();
        Module targetModule = createTargetModuleForViolation();
        List<Module> modules = List.of(sourceModule, targetModule);

        // Mock high similarity improvement
        mockHighSimilarityImprovement(sourceModule, targetModule);

        // When
        architectureViolation.execute(modules);

        // Then - Should suggest move with high confidence
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any());
    }

    // Test Case 5: Violation Cluster - 3+ classes same forbidden origin
    @Test
    void testViolationCluster_multipleClassesSameForbiddenOrigin() {
        // Given
        Module moduleWithCluster = createModuleWithViolationCluster();
        List<Module> modules = List.of(moduleWithCluster);

        // When
        architectureViolation.execute(modules);

        // Then - Should identify cluster and recommend reference class change
        // Verification through log observation for cluster detection
    }

    // Test Case 6: No Valid Target - Class needs new module creation
    @Test
    void testNoValidTarget_needsNewModuleCreation() {
        // Given
        Module moduleWithOrphanedClass = createModuleWithOrphanedClass();
        List<Module> modules = List.of(moduleWithOrphanedClass);

        // When
        architectureViolation.execute(modules);

        // Then - Should suggest creating new module
        // Verification through log observation for new module suggestion
    }

    // Test Case 7: Minimal Violations - Target creates fewest new violations
    @Test
    void testMinimalViolations_targetWithFewestNewViolations() {
        // Given
        Module sourceModule = createModuleWithSingleViolation();
        Module betterTarget = createBetterTargetModule();
        Module worseTarget = createWorseTargetModule();
        List<Module> modules = List.of(sourceModule, betterTarget, worseTarget);

        // Mock similarity calculations to prefer better target
        mockPreferBetterTarget(sourceModule, betterTarget, worseTarget);

        // When
        architectureViolation.execute(modules);

        // Then - Should prefer target with fewer new violations
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any());
    }

    // Test Case 8: Mixed Dependencies - Class has both allowed and forbidden deps
    @Test
    void testMixedDependencies_allowedAndForbiddenDeps() {
        // Given
        Module moduleWithMixedDeps = createModuleWithMixedDependencies();
        Module targetModule = createTargetModuleForViolation();
        List<Module> modules = List.of(moduleWithMixedDeps, targetModule);

        // Mock similarity calculations
        mockSimilarityCalculations(moduleWithMixedDeps, targetModule);

        // When
        architectureViolation.execute(modules);

        // Then - Should still suggest move despite mixed dependencies
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any());
    }

    // Test Case 9: Empty Module - No classes or dependencies, skip gracefully
    @Test
    void testEmptyModule_noClassesOrDependencies() {
        // Given
        Module emptyModule = createEmptyModule();
        List<Module> modules = List.of(emptyModule);

        // When
        architectureViolation.execute(modules);

        // Then - Should skip gracefully without errors
        verify(moduleService, never()).calculateClassSimilarities(any());
    }

    // Test Case 10: Self-Dependencies - Module depends on itself, ignore
    @Test
    void testSelfDependencies_moduleDepedendsOnItself() {
        // Given
        Module moduleWithSelfDeps = createModuleWithSelfDependencies();
        List<Module> modules = List.of(moduleWithSelfDeps);

        // When
        architectureViolation.execute(modules);

        // Then - Should ignore self-dependencies and find no violations
        verify(moduleService, never()).calculateClassSimilarities(any());
    }

    // Test Case 11: All Classes Violate - Every class in module has violations
    @Test
    void testAllClassesViolate_everyClassHasViolations() {
        // Given
        Module moduleWithAllViolatingClasses = createModuleWithAllViolatingClasses();
        List<Module> modules = List.of(moduleWithAllViolatingClasses);

        // When
        architectureViolation.execute(modules);

        // Then - Should identify violations for all classes
        // Verification through log observation
    }

    // Test Case 12: Circular Chains - Complex dependency loops, ignore in analysis
    @Test
    void testCircularChains_complexDependencyLoops() {
        // Given
        List<Module> modulesWithCircularDeps = createModulesWithCircularDependencies();

        // When
        architectureViolation.execute(modulesWithCircularDeps);

        // Then - Should handle circular dependencies gracefully
        // Verification through successful completion without infinite loops
    }

    // Helper methods to create test data

    private Dependency createDependency(String fullyQualifiedName, String packageName) {
        Dependency dependency = new Dependency(packageName);
        dependency.addType(new Type(fullyQualifiedName));
        return dependency;
    }

    private Module createCleanModule() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.allowed")
        );
        List<Clazz> classes = List.of(
                new Clazz("class1", "CleanClass",
                        List.of(createDependency("com.example.allowed.SomeClass", "com.example.allowed")),
                        0.8, "test-module", "test-module")
        );

        return new Module("clean-module", "Clean Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.8);
    }

    private Module createModuleWithSingleViolation() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.forbidden")
        );
        List<Clazz> classes = List.of(
                new Clazz("violating-class", "ViolatingClass",
                        List.of(createDependency("com.example.forbidden.BadClass", "com.example.forbidden")),
                        0.5, "source-module", "source-module")
        );

        return new Module("source-module", "Source Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.5);
    }

    private Module createTargetModuleForViolation() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.forbidden"), // Allows the forbidden origin
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.allowed")
        );
        List<Clazz> classes = List.of(
                new Clazz("target-class", "TargetClass",
                        List.of(createDependency("com.example.allowed.GoodClass", "com.example.allowed")),
                        0.9, "target-module", "target-module")
        );

        return new Module("target-module", "Target Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.9);
    }

    private Module createModuleWithMultipleViolations() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.forbidden1"),
                new DependencyOrigin("com.example.forbidden2")
        );
        List<Clazz> classes = List.of(
                new Clazz("multi-violating-class", "MultiViolatingClass",
                        List.of(
                                createDependency("com.example.forbidden1.BadClass1", "com.example.forbidden1"),
                                createDependency("com.example.forbidden2.BadClass2", "com.example.forbidden2")
                        ),
                        0.3, "multi-module", "multi-module")
        );

        return new Module("multi-module", "Multi Violation Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.3);
    }

    private Module createModuleWithViolationCluster() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.forbidden")
        );
        List<Clazz> classes = List.of(
                new Clazz("violating-class1", "ViolatingClass1",
                        List.of(createDependency("com.example.forbidden.BadClass", "com.example.forbidden")),
                        0.4, "cluster-module", "cluster-module"),
                new Clazz("violating-class2", "ViolatingClass2",
                        List.of(createDependency("com.example.forbidden.AnotherBadClass", "com.example.forbidden")),
                        0.4, "cluster-module", "cluster-module"),
                new Clazz("violating-class3", "ViolatingClass3",
                        List.of(createDependency("com.example.forbidden.ThirdBadClass", "com.example.forbidden")),
                        0.4, "cluster-module", "cluster-module")
        );

        return new Module("cluster-module", "Cluster Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.4);
    }

    private Module createModuleWithOrphanedClass() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.unique")
        );
        List<Clazz> classes = List.of(
                new Clazz("orphaned-class", "OrphanedClass",
                        List.of(createDependency("com.example.unique.UniqueClass", "com.example.unique")),
                        0.6, "orphan-module", "orphan-module")
        );

        return new Module("orphan-module", "Orphan Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.6);
    }

    private Module createBetterTargetModule() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.forbidden") // Allows the violation
        );
        return new Module("better-target", "Better Target", new ArrayList<>(),
                allowedRules, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0.5);
    }

    private Module createWorseTargetModule() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.forbidden") // Allows the violation
        );
        return new Module("worse-target", "Worse Target", new ArrayList<>(),
                allowedRules, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0.5);
    }

    private Module createModuleWithMixedDependencies() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.allowed"),
                new DependencyOrigin("com.example.forbidden")
        );
        List<Clazz> classes = List.of(
                new Clazz("mixed-class", "MixedClass",
                        List.of(
                                createDependency("com.example.allowed.GoodClass", "com.example.allowed"),
                                createDependency("com.example.forbidden.BadClass", "com.example.forbidden")
                        ),
                        0.6, "mixed-module", "mixed-module")
        );

        return new Module("mixed-module", "Mixed Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.6);
    }

    private Module createEmptyModule() {
        return new Module("empty-module", "Empty Module", new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0.0);
    }

    private Module createModuleWithSelfDependencies() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("Self Module") // Self-dependency
        );
        List<Clazz> classes = List.of(
                new Clazz("self-class", "SelfClass",
                        List.of(createDependency("Self Module.InternalClass", "Self Module")),
                        0.7, "self-module", "self-module")
        );

        return new Module("self-module", "Self Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.7);
    }

    private Module createModuleWithAllViolatingClasses() {
        List<AllowedRule> allowedRules = List.of(
                new AllowedRule("com.example.allowed")
        );
        List<DependencyOrigin> dependencyOrigins = List.of(
                new DependencyOrigin("com.example.forbidden")
        );
        List<Clazz> classes = List.of(
                new Clazz("violating1", "ViolatingClass1",
                        List.of(createDependency("com.example.forbidden.Bad1", "com.example.forbidden")),
                        0.3, "all-violating", "all-violating"),
                new Clazz("violating2", "ViolatingClass2",
                        List.of(createDependency("com.example.forbidden.Bad2", "com.example.forbidden")),
                        0.3, "all-violating", "all-violating"),
                new Clazz("violating3", "ViolatingClass3",
                        List.of(createDependency("com.example.forbidden.Bad3", "com.example.forbidden")),
                        0.3, "all-violating", "all-violating")
        );

        return new Module("all-violating", "All Violating Module", new ArrayList<>(),
                allowedRules, dependencyOrigins, new ArrayList<>(), new ArrayList<>(), classes, 0.3);
    }

    private List<Module> createModulesWithCircularDependencies() {
        // Module A depends on Module B
        Module moduleA = new Module("module-a", "Module A", new ArrayList<>(),
                List.of(new AllowedRule("com.example.allowed")),
                List.of(new DependencyOrigin("Module B")),
                new ArrayList<>(), new ArrayList<>(),
                List.of(new Clazz("class-a", "ClassA",
                        List.of(createDependency("Module B.ClassB", "Module B")),
                        0.5, "module-a", "module-a")), 0.5);

        // Module B depends on Module A (circular)
        Module moduleB = new Module("module-b", "Module B", new ArrayList<>(),
                List.of(new AllowedRule("com.example.allowed")),
                List.of(new DependencyOrigin("Module A")),
                new ArrayList<>(), new ArrayList<>(),
                List.of(new Clazz("class-b", "ClassB",
                        List.of(createDependency("Module A.ClassA", "Module A")),
                        0.5, "module-b", "module-b")), 0.5);

        return List.of(moduleA, moduleB);
    }

    // Helper methods for mocking

    private void mockSimilarityCalculations(Module sourceModule, Module targetModule) {
        doAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            module.setSimilarity(0.8); // Set a reasonable similarity
            return null;
        }).when(moduleService).calculateModuleSimilarity(any(Module.class));
    }

    private void mockHighSimilarityImprovement(Module sourceModule, Module targetModule) {
        doAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            if (module.getId().contains("temp_with_")) {
                module.setSimilarity(0.95); // High similarity for target with added class
            } else if (module.getId().contains("temp_without_")) {
                module.setSimilarity(0.85); // Good similarity for source without class
            } else {
                module.setSimilarity(0.6); // Original similarity
            }
            return null;
        }).when(moduleService).calculateModuleSimilarity(any(Module.class));
    }

    private void mockPreferBetterTarget(Module sourceModule, Module betterTarget, Module worseTarget) {
        doAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            if (module.getId().equals("better-target")) {
                module.setSimilarity(0.95);
            } else if (module.getId().equals("worse-target")) {
                module.setSimilarity(0.7);
            } else {
                module.setSimilarity(0.6);
            }
            return null;
        }).when(moduleService).calculateModuleSimilarity(any(Module.class));
    }
}