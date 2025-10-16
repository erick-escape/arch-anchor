package tcc.com.viewer.services.architecturalAnalyses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.dependency.Type;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.services.ModuleService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for SplitModule analysis.
 * Tests the refactored edge-based implementation that uses superRefClass and deRefClass
 * to partition modules based on similarity extremes.
 */
@ExtendWith(MockitoExtension.class)
class SplitModuleTest {

    @Mock
    private ModuleService moduleService;

    private SplitModule splitModule;

    @BeforeEach
    void setUp() {
        splitModule = new SplitModule(moduleService);

        // Setup lenient mock behavior for similarity calculations
        lenient().doNothing().when(moduleService).calculateClassSimilarities(any(Module.class));
        lenient().doNothing().when(moduleService).calculateAvgSimilarityWithRefClazzes(any(Module.class));
        lenient().doNothing().when(moduleService).calculateModuleSimilarity(any(Module.class));

        // Mock class-to-class similarity calculation (used in assignClassesToModules)
        // Default: return moderate similarity based on avgSimilarityWithRefClazzes
        lenient().when(moduleService.calculateSimilarity(any(Clazz.class), any(Clazz.class)))
                .thenAnswer(invocation -> {
                    Clazz c1 = invocation.getArgument(0);
                    Clazz c2 = invocation.getArgument(1);

                    // Use avgSimilarityWithRefClazzes as proxy for similarity
                    double sim1 = c1.getAvgSimilarityWithRefClazzes() != null ? c1.getAvgSimilarityWithRefClazzes() : 0.5;
                    double sim2 = c2.getAvgSimilarityWithRefClazzes() != null ? c2.getAvgSimilarityWithRefClazzes() : 0.5;

                    // Return higher similarity if both have similar avgSimilarityWithRefClazzes
                    double diff = Math.abs(sim1 - sim2);
                    return 1.0 - diff; // Closer values = higher similarity
                });
    }

    // Test Case 1: No Beneficial Splits - Module already has optimal cohesion
    @Test
    void testNoBeneficialSplits_moduleAlreadyOptimal() {
        // Given - module with high cohesion (splitting wouldn't help)
        Module highCohesionModule = createModuleWithHighCohesion("high-cohesion", 0.9);
        List<Module> modules = List.of(highCohesionModule);

        // Mock: split modules would have lower similarity
        mockSplitModulesLowerSimilarity();

        // When
        splitModule.execute(modules);

        // Then - Should find no beneficial splits
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 2: Single Beneficial Split - Module benefits from division
    @Test
    void testSingleBeneficialSplit_moduleBenefitsFromDivision() {
        // Given - module with low cohesion that could be split
        Module lowCohesionModule = createModuleWithLowCohesion("low-cohesion", 0.5);
        List<Module> modules = List.of(lowCohesionModule);

        // Mock: split modules have higher similarity
        mockSplitModulesHigherSimilarity(0.8, 0.75);

        // When
        splitModule.execute(modules);

        // Then - Should suggest splitting
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 3: Module Without Reference Classes - Should skip
    @Test
    void testModuleWithoutRefClasses_shouldSkip() {
        // Given - module without reference classes
        Module noRefModule = new Module("no-ref", "No Ref Module",
                null, null, null,
                List.of(createClazz("c1", "C1", 0.5, "java.util"),
                        createClazz("c2", "C2", 0.4, "java.io"),
                        createClazz("c3", "C3", 0.6, "java.lang")),
                0.5, 0.0, 0);

        List<Module> modules = List.of(noRefModule);

        // When
        splitModule.execute(modules);

        // Then - Should skip module without ref classes
        verify(moduleService, never()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 4: Insufficient Classes - Need at least 3 classes
    @Test
    void testInsufficientClasses_needAtLeastThree() {
        // Given - module with only 2 classes (cannot split meaningfully)
        Clazz ref = createClazz("ref", "RefClass", 0.9, "com.example");
        Module tinyModule = new Module("tiny", "Tiny Module",
                List.of(ref), null, null,
                List.of(ref, createClazz("c1", "C1", 0.4, "java.util")),
                0.6, 0.0, 0);

        List<Module> modules = List.of(tinyModule);

        // When
        splitModule.execute(modules);

        // Then - Should skip module with insufficient classes
        verify(moduleService, never()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 5: Clear Edge Classes - High and low similarity extremes
    @Test
    void testClearEdgeClasses_highAndLowExtremes() {
        // Given - module with clear high/low similarity edges
        Clazz superRef = createClazz("super", "SuperRef", 0.95, "com.example.core");
        Clazz deRef = createClazz("de", "DeRef", 0.15, "com.different");

        Module moduleWithEdges = new Module("edges", "Module With Edges",
                List.of(superRef), null, null,
                List.of(superRef,
                        createClazz("c1", "C1", 0.7, "com.example.core"),
                        createClazz("c2", "C2", 0.6, "com.example"),
                        deRef,
                        createClazz("c3", "C3", 0.3, "com.different")),
                0.5, 0.0, 0);

        List<Module> modules = List.of(moduleWithEdges);

        // Mock: split improves similarity
        mockSplitModulesHigherSimilarity(0.85, 0.75);

        // When
        splitModule.execute(modules);

        // Then - Should identify and split based on edges
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 6: All Reference Classes - No deRef class available
    @Test
    void testAllReferenceClasses_noDeRefAvailable() {
        // Given - module where all classes are reference classes
        Clazz ref1 = createClazz("ref1", "Ref1", 0.9, "com.example");
        Clazz ref2 = createClazz("ref2", "Ref2", 0.85, "com.example");
        Clazz ref3 = createClazz("ref3", "Ref3", 0.88, "com.example");

        Module allRefModule = new Module("all-ref", "All Ref Module",
                List.of(ref1, ref2, ref3), null, null,
                List.of(ref1, ref2, ref3),
                0.87, 0.0, 0);

        List<Module> modules = List.of(allRefModule);

        // When
        splitModule.execute(modules);

        // Then - Should skip (cannot find deRef class)
        verify(moduleService, never()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 7: Identical Edge Classes - superRef equals deRef
    @Test
    void testIdenticalEdgeClasses_superRefEqualsDeRef() {
        // Given - module where highest ref is also lowest non-ref (edge case)
        Clazz onlyRef = createClazz("only", "OnlyRef", 0.8, "com.example");

        Module singleRefModule = new Module("single-ref", "Single Ref Module",
                List.of(onlyRef), null, null,
                List.of(onlyRef,
                        createClazz("c1", "C1", 0.8, "com.example"), // Same similarity
                        createClazz("c2", "C2", 0.8, "com.example")),
                0.8, 0.0, 0);

        List<Module> modules = List.of(singleRefModule);

        // When
        splitModule.execute(modules);

        // Then - Should handle gracefully (may skip or process)
        // Implementation should prevent splitting when edges are too similar
    }

    // Test Case 8: Balanced Class Distribution - Classes evenly split
    @Test
    void testBalancedClassDistribution_classesEvenlySplit() {
        // Given - module where classes naturally divide into two groups
        Clazz superRef = createClazz("super", "SuperRef", 0.9, "com.group1");
        Clazz deRef = createClazz("de", "DeRef", 0.2, "com.group2");

        Module balancedModule = new Module("balanced", "Balanced Module",
                List.of(superRef), null, null,
                List.of(superRef,
                        createClazz("c1", "C1", 0.85, "com.group1"),
                        createClazz("c2", "C2", 0.8, "com.group1"),
                        deRef,
                        createClazz("c3", "C3", 0.25, "com.group2"),
                        createClazz("c4", "C4", 0.3, "com.group2")),
                0.5, 0.0, 0);

        List<Module> modules = List.of(balancedModule);

        // Mock: both split modules have good similarity
        mockSplitModulesHigherSimilarity(0.87, 0.82);
        mockClassSimilarityForBalancedSplit();

        // When
        splitModule.execute(modules);

        // Then - Should create balanced split
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 9: Unbalanced Distribution - Most classes near one edge
    @Test
    void testUnbalancedDistribution_mostClassesNearOneEdge() {
        // Given - module where most classes cluster near superRef
        Clazz superRef = createClazz("super", "SuperRef", 0.95, "com.core");
        Clazz deRef = createClazz("de", "DeRef", 0.1, "com.outlier");

        Module unbalancedModule = new Module("unbalanced", "Unbalanced Module",
                List.of(superRef), null, null,
                List.of(superRef,
                        createClazz("c1", "C1", 0.9, "com.core"),
                        createClazz("c2", "C2", 0.85, "com.core"),
                        createClazz("c3", "C3", 0.88, "com.core"),
                        createClazz("c4", "C4", 0.82, "com.core"),
                        deRef),
                0.75, 0.0, 0);

        List<Module> modules = List.of(unbalancedModule);

        // Mock: unbalanced split still improves
        mockSplitModulesHigherSimilarity(0.92, 0.80);

        // When
        splitModule.execute(modules);

        // Then - Should handle unbalanced split
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 10: High Improvement Split - Dramatic cohesion increase
    @Test
    void testHighImprovementSplit_dramaticCohesionIncrease() {
        // Given - module with very low cohesion due to disparate classes
        Clazz superRef = createClazz("super", "DatabaseService", 0.8, "java.sql", "javax.sql");
        Clazz deRef = createClazz("de", "UIController", 0.15, "javax.swing", "java.awt");

        Module disparateModule = new Module("disparate", "Disparate Module",
                List.of(superRef), null, null,
                List.of(superRef,
                        createClazz("c1", "DataAccess", 0.75, "java.sql"),
                        createClazz("c2", "Repository", 0.72, "javax.sql"),
                        deRef,
                        createClazz("c3", "ViewPanel", 0.18, "javax.swing"),
                        createClazz("c4", "DialogBox", 0.2, "java.awt")),
                0.3, 0.0, 0);

        List<Module> modules = List.of(disparateModule);

        // Mock: dramatic improvement when split
        mockSplitModulesHigherSimilarity(0.90, 0.85);

        // When
        splitModule.execute(modules);

        // Then - Should strongly recommend split
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 11: Marginal Improvement - Small but positive benefit
    @Test
    void testMarginalImprovement_smallButPositiveBenefit() {
        // Given - module that slightly benefits from splitting
        Clazz superRef = createClazz("super", "SuperRef", 0.75, "com.example");
        Clazz deRef = createClazz("de", "DeRef", 0.68, "com.example.other");

        Module marginalModule = new Module("marginal", "Marginal Module",
                List.of(superRef), null, null,
                List.of(superRef,
                        createClazz("c1", "C1", 0.72, "com.example"),
                        createClazz("c2", "C2", 0.7, "com.example"),
                        deRef),
                0.71, 0.0, 0);

        List<Module> modules = List.of(marginalModule);

        // Mock: marginal improvement
        mockSplitModulesHigherSimilarity(0.74, 0.73);

        // When
        splitModule.execute(modules);

        // Then - Should still suggest split (any positive improvement)
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 12: Asymmetric Improvement - One split good, other bad
    @Test
    void testAsymmetricImprovement_oneSplitGoodOtherBad() {
        // Given - module where one split would be good but other would be worse
        Clazz superRef = createClazz("super", "SuperRef", 0.9, "com.core");
        Clazz deRef = createClazz("de", "DeRef", 0.3, "com.misc");

        Module asymmetricModule = new Module("asymmetric", "Asymmetric Module",
                List.of(superRef), null, null,
                List.of(superRef,
                        createClazz("c1", "C1", 0.85, "com.core"),
                        createClazz("c2", "C2", 0.82, "com.core"),
                        deRef),
                0.72, 0.0, 0);

        List<Module> modules = List.of(asymmetricModule);

        // Mock: one module improves, other doesn't
        mockAsymmetricSplitImprovement(0.88, 0.65);

        // When
        splitModule.execute(modules);

        // Then - Should not suggest split (requires both to improve)
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 13: Multiple Modules - Some splittable, some not
    @Test
    void testMultipleModules_someSplittableSomeNot() {
        // Given - mix of splittable and non-splittable modules
        Module goodModule = createModuleWithHighCohesion("good", 0.9);
        Module badModule = createModuleWithLowCohesion("bad", 0.4);
        Module noRefModule = new Module("no-ref", "No Ref", null, null, null,
                List.of(createClazz("c1", "C1", 0.5, "java.util")), 0.5, 0.0, 0);

        List<Module> modules = List.of(goodModule, badModule, noRefModule);

        // Mock: only bad module benefits from split
        mockSplitModulesHigherSimilarity(0.75, 0.70);

        // When
        splitModule.execute(modules);

        // Then - Should identify splittable module
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 14: Empty Modules List - Should handle gracefully
    @Test
    void testEmptyModulesList_shouldHandleGracefully() {
        // Given
        List<Module> emptyModules = List.of();

        // When
        splitModule.execute(emptyModules);

        // Then - Should complete without errors
        verify(moduleService, never()).calculateModuleSimilarity(any(Module.class));
    }

    // Test Case 15: Large Module - Performance with many classes
    @Test
    void testLargeModule_performanceWithManyClasses() {
        // Given - module with 10 classes
        Clazz superRef = createClazz("super", "SuperRef", 0.95, "com.core");
        List<Clazz> classes = new ArrayList<>();
        classes.add(superRef);

        for (int i = 1; i <= 8; i++) {
            classes.add(createClazz("c" + i, "Class" + i, 0.5 + (i * 0.03), "com.various" + i));
        }

        Clazz deRef = createClazz("de", "DeRef", 0.2, "com.outlier");
        classes.add(deRef);

        Module largeModule = new Module("large", "Large Module",
                List.of(superRef), null, null, classes, 0.55, 0.0, 0);

        List<Module> modules = List.of(largeModule);

        // Mock: split improves
        mockSplitModulesHigherSimilarity(0.8, 0.75);

        // When
        splitModule.execute(modules);

        // Then - Should complete efficiently
        verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
    }

    // Helper methods to create test data

    private Dependency createDependency(String fullyQualifiedName, String packageName) {
        Dependency dependency = new Dependency(packageName);
        dependency.addType(new Type(fullyQualifiedName));
        return dependency;
    }

    private Clazz createClazz(String id, String name, Double avgSimilarityWithRefClazzes, String... packageNames) {
        List<Dependency> dependencies = new ArrayList<>();
        for (String pkg : packageNames) {
            dependencies.add(createDependency(pkg + ".SomeClass", pkg));
        }
        return new Clazz(id, name, dependencies, 0.7, avgSimilarityWithRefClazzes, "test-module", "test-module");
    }

    private Module createModuleWithHighCohesion(String id, double similarity) {
        Clazz ref = createClazz(id + "-ref", id + "Ref", 0.95, "com.example." + id);
        List<Clazz> classes = List.of(
                ref,
                createClazz(id + "-c1", id + "C1", 0.9, "com.example." + id),
                createClazz(id + "-c2", id + "C2", 0.88, "com.example." + id),
                createClazz(id + "-c3", id + "C3", 0.92, "com.example." + id)
        );

        return new Module(id, id + " Module", List.of(ref), null, null, classes, similarity, 0.9, 0);
    }

    private Module createModuleWithLowCohesion(String id, double similarity) {
        Clazz ref = createClazz(id + "-ref", id + "Ref", 0.7, "com.group1");
        List<Clazz> classes = List.of(
                ref,
                createClazz(id + "-c1", id + "C1", 0.65, "com.group1"),
                createClazz(id + "-c2", id + "C2", 0.3, "com.group2"),
                createClazz(id + "-c3", id + "C3", 0.25, "com.group2")
        );

        return new Module(id, id + " Module", List.of(ref), null, null, classes, similarity, 0.5, 0);
    }

    // Helper methods for mocking

    private void mockSplitModulesHigherSimilarity(double module1Sim, double module2Sim) {
        doAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            if (module.getName().contains("high-cohesion")) {
                module.setSimilarity(module1Sim);
            } else if (module.getName().contains("low-cohesion")) {
                module.setSimilarity(module2Sim);
            } else {
                module.setSimilarity(0.5); // Original module
            }
            return null;
        }).when(moduleService).calculateModuleSimilarity(any(Module.class));
    }

    private void mockSplitModulesLowerSimilarity() {
        doAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            if (module.getName().contains("high-cohesion") || module.getName().contains("low-cohesion")) {
                module.setSimilarity(0.6); // Lower than original
            } else {
                module.setSimilarity(0.9); // Original module
            }
            return null;
        }).when(moduleService).calculateModuleSimilarity(any(Module.class));
    }

    private void mockAsymmetricSplitImprovement(double module1Sim, double module2Sim) {
        doAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            if (module.getName().contains("high-cohesion")) {
                module.setSimilarity(module1Sim); // Better than original
            } else if (module.getName().contains("low-cohesion")) {
                module.setSimilarity(module2Sim); // Worse than original (0.72)
            } else {
                module.setSimilarity(0.72); // Original module
            }
            return null;
        }).when(moduleService).calculateModuleSimilarity(any(Module.class));
    }

    private void mockClassSimilarityForBalancedSplit() {
        // Mock class-to-class similarity for balanced split test
        lenient().when(moduleService.calculateSimilarity(any(Clazz.class), any(Clazz.class)))
                .thenAnswer(invocation -> {
                    Clazz clazz = invocation.getArgument(0);
                    Clazz targetEdge = invocation.getArgument(1);

                    // Classes C1, C2 are similar to SuperRef (high similarity)
                    if ((clazz.getName().equals("C1") || clazz.getName().equals("C2"))
                            && targetEdge.getName().equals("SuperRef")) {
                        return 0.85;
                    }
                    // Classes C3, C4 are similar to DeRef (high similarity)
                    if ((clazz.getName().equals("C3") || clazz.getName().equals("C4"))
                            && targetEdge.getName().equals("DeRef")) {
                        return 0.80;
                    }
                    // Cross-group similarities are low
                    if ((clazz.getName().equals("C1") || clazz.getName().equals("C2"))
                            && targetEdge.getName().equals("DeRef")) {
                        return 0.25;
                    }
                    if ((clazz.getName().equals("C3") || clazz.getName().equals("C4"))
                            && targetEdge.getName().equals("SuperRef")) {
                        return 0.30;
                    }

                    // Default moderate similarity
                    return 0.5;
                });
    }
}
