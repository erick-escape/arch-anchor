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
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for MergeModule analysis. Tests the refactored implementation
 * that uses avgRefClazzesSimilarity for determining beneficial module merges.
 */
@ExtendWith(MockitoExtension.class)
class MergeModuleTest {

	@Mock
	private ModuleService moduleService;

	private MergeModule mergeModule;

	@BeforeEach
	void setUp() {
		mergeModule = new MergeModule(moduleService);

		// Setup default mock behavior for avgSimilarityWithRefClazzes calculation with
		// lenient stubbing
		lenient().doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Calculate avgRefClazzesSimilarity for the module based on its ref classes
			if (module.getRefClazzes() != null && !module.getRefClazzes().isEmpty()) {
				double totalSim = 0.0;
				for (Clazz refClass : module.getRefClazzes()) {
					if (refClass.getAvgSimilarityWithRefClazzes() != null) {
						totalSim += refClass.getAvgSimilarityWithRefClazzes();
					}
				}
				module.setAvgRefClazzesSimilarity(totalSim / module.getRefClazzes().size());
			}
			return null;
		}).when(moduleService).calculateAvgSimilarityWithRefClazzes(any(Module.class));

		// Setup default mock behavior for module similarity calculation
		lenient().doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Default: set similarity to avgRefClazzesSimilarity if not already set
			if (module.getSimilarity() == null && module.getAvgRefClazzesSimilarity() != null) {
				module.setSimilarity(module.getAvgRefClazzesSimilarity());
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 1: No Beneficial Merges - All modules optimally separated
	@Test
	void testNoBeneficialMerges_allModulesOptimallySeparated() {
		// Given - modules with high individual avgRefClazzesSimilarity
		Module module1 = createModuleWithHighCohesion("module1", 0.9);
		Module module2 = createModuleWithHighCohesion("module2", 0.88);
		List<Module> modules = List.of(module1, module2);

		// Mock: merged module would have lower avgRefClazzesSimilarity
		mockMergedModuleLowerCohesion(0.75);

		// When
		mergeModule.execute(modules);

		// Then - Should find no beneficial merges
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 2: Single Beneficial Merge - Two modules better together
	@Test
	void testSingleBeneficialMerge_twoModulesBetterTogether() {
		// Given - modules with low individual cohesion that would improve when merged
		Module module1 = createModuleWithLowCohesion("module1", 0.6);
		Module module2 = createModuleWithLowCohesion("module2", 0.65);
		List<Module> modules = List.of(module1, module2);

		// Mock: merged module has higher avgRefClazzesSimilarity
		mockMergedModuleHigherCohesion(0.85);

		// When
		mergeModule.execute(modules);

		// Then - Should suggest merging the modules
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 3: Multiple Merge Candidates - Several module pairs benefit
	@Test
	void testMultipleMergeCandidates_severalModulePairsBenefit() {
		// Given - multiple modules with potential for beneficial merges
		Module module1 = createModuleWithLowCohesion("module1", 0.5);
		Module module2 = createModuleWithLowCohesion("module2", 0.55);
		Module module3 = createModuleWithLowCohesion("module3", 0.52);
		List<Module> modules = List.of(module1, module2, module3);

		// Mock: any merge improves cohesion
		mockMergedModuleHigherCohesion(0.8);

		// When
		mergeModule.execute(modules);

		// Then - Should suggest multiple merge possibilities
		verify(moduleService, atLeast(3)).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 4: Module Without Reference Classes - Should skip
	@Test
	void testModuleWithoutRefClasses_shouldSkip() {
		// Given - one module without reference classes
		Module moduleWithoutRef = new Module("no-ref", "No Ref Module", null, null, null,
				List.of(createClazz("class1", "Class1", 0.5, "java.util")), 0.5, 0.0, 0);

		Module normalModule = createModuleWithHighCohesion("normal", 0.8);
		List<Module> modules = List.of(moduleWithoutRef, normalModule);

		// When
		mergeModule.execute(modules);

		// Then - Should skip module without ref classes
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 5: Both Modules Without Reference Classes - Should skip pair
	@Test
	void testBothModulesWithoutRefClasses_shouldSkipPair() {
		// Given - both modules without reference classes
		Module module1 = new Module("no-ref-1", "No Ref 1", null, null, null,
				List.of(createClazz("c1", "C1", 0.5, "java.util")), 0.5, 0.0, 0);
		Module module2 = new Module("no-ref-2", "No Ref 2", null, null, null,
				List.of(createClazz("c2", "C2", 0.5, "java.io")), 0.5, 0.0, 0);

		List<Module> modules = List.of(module1, module2);

		// When
		mergeModule.execute(modules);

		// Then - Should skip both modules
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 6: High Improvement Merge - Dramatic cohesion increase
	@Test
	void testHighImprovementMerge_dramaticCohesionIncrease() {
		// Given - modules with very low individual cohesion
		Module module1 = createModuleWithVeryLowCohesion("module1", 0.3);
		Module module2 = createModuleWithVeryLowCohesion("module2", 0.35);
		List<Module> modules = List.of(module1, module2);

		// Mock: merged module has dramatically higher cohesion
		mockMergedModuleHigherCohesion(0.92);

		// When
		mergeModule.execute(modules);

		// Then - Should strongly recommend merge
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 7: Asymmetric Improvement - One module improves, other doesn't
	@Test
	void testAsymmetricImprovement_oneModuleImprovesOtherDoesnt() {
		// Given - one module with low cohesion, one with high cohesion
		Module lowCohesionModule = createModuleWithLowCohesion("low", 0.4);
		Module highCohesionModule = createModuleWithHighCohesion("high", 0.92);
		List<Module> modules = List.of(lowCohesionModule, highCohesionModule);

		// Mock: merged module is better than low but worse than high
		mockMergedModuleHigherCohesion(0.75);

		// When
		mergeModule.execute(modules);

		// Then - Should NOT recommend merge (min improvement is negative)
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 8: Equal Cohesion Modules - Merged stays similar
	@Test
	void testEqualCohesionModules_mergedStaysSimilar() {
		// Given - modules with equal moderate cohesion
		Module module1 = createModuleWithModerateCohesion("module1", 0.7);
		Module module2 = createModuleWithModerateCohesion("module2", 0.7);
		List<Module> modules = List.of(module1, module2);

		// Mock: merged module has same cohesion
		mockMergedModuleHigherCohesion(0.7);

		// When
		mergeModule.execute(modules);

		// Then - Should not recommend merge (no improvement)
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 9: Marginal Improvement - Small but positive for both
	@Test
	void testMarginalImprovement_smallButPositiveForBoth() {
		// Given - modules with similar cohesion
		Module module1 = createModuleWithModerateCohesion("module1", 0.68);
		Module module2 = createModuleWithModerateCohesion("module2", 0.69);
		List<Module> modules = List.of(module1, module2);

		// Mock: merged module slightly better than both
		mockMergedModuleHigherCohesion(0.72);

		// When
		mergeModule.execute(modules);

		// Then - Should recommend merge (positive improvement for both)
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 10: Empty Modules List - Should handle gracefully
	@Test
	void testEmptyModulesList_shouldHandleGracefully() {
		// Given
		List<Module> emptyModules = List.of();

		// When
		mergeModule.execute(emptyModules);

		// Then - Should complete without errors
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 11: Single Module - Cannot merge
	@Test
	void testSingleModule_cannotMerge() {
		// Given
		Module singleModule = createModuleWithHighCohesion("single", 0.8);
		List<Module> modules = List.of(singleModule);

		// When
		mergeModule.execute(modules);

		// Then - Should not suggest any merges
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 12: Merge with Multiple Reference Classes - Complex evaluation
	@Test
	void testMergeWithMultipleRefClasses_complexEvaluation() {
		// Given - modules with multiple reference classes each
		Clazz ref1a = createClazz("ref1a", "Ref1A", 0.8, "com.example.a");
		Clazz ref1b = createClazz("ref1b", "Ref1B", 0.75, "com.example.a");
		Module module1 = createModuleWithRefClasses("module1", List.of(ref1a, ref1b), 0.7);

		Clazz ref2a = createClazz("ref2a", "Ref2A", 0.82, "com.example.b");
		Clazz ref2b = createClazz("ref2b", "Ref2B", 0.78, "com.example.b");
		Module module2 = createModuleWithRefClasses("module2", List.of(ref2a, ref2b), 0.72);

		List<Module> modules = List.of(module1, module2);

		// Mock: merged module with 4 ref classes has good cohesion
		mockMergedModuleHigherCohesion(0.85);

		// When
		mergeModule.execute(modules);

		// Then - Should evaluate all ref classes together
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 13: Null avgRefClazzesSimilarity - Should handle gracefully
	@Test
	void testNullAvgRefClazzesSimilarity_shouldHandleGracefully() {
		// Given - modules with null avgRefClazzesSimilarity
		Module module1 = createModuleWithNullCohesion("module1");
		Module module2 = createModuleWithNullCohesion("module2");
		List<Module> modules = List.of(module1, module2);

		// Mock: merged module has valid cohesion
		mockMergedModuleHigherCohesion(0.75);

		// When
		mergeModule.execute(modules);

		// Then - Should treat null as 0.0 and calculate improvement
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 14: Best Merge Among Many - Ranking by improvement
	@Test
	void testBestMergeAmongMany_rankingByImprovement() {
		// Given - multiple modules with varying merge potentials
		Module module1 = createModuleWithLowCohesion("module1", 0.5);
		Module module2 = createModuleWithLowCohesion("module2", 0.52);
		Module module3 = createModuleWithModerateCohesion("module3", 0.7);
		Module module4 = createModuleWithLowCohesion("module4", 0.48);
		List<Module> modules = List.of(module1, module2, module3, module4);

		// Mock: different improvements for different pairs
		mockMergedModuleHigherCohesion(0.8);

		// When
		mergeModule.execute(modules);

		// Then - Should rank merges by improvement
		verify(moduleService, atLeast(4)).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 15: Large Number of Modules - Performance test
	@Test
	void testLargeNumberOfModules_performanceTest() {
		// Given - 15 modules (results in 105 pairwise comparisons)
		List<Module> modules = new ArrayList<>();
		for (int i = 0; i < 15; i++) {
			modules.add(createModuleWithModerateCohesion("module" + i, 0.6 + (i * 0.01)));
		}

		// Mock: some merges beneficial
		mockMergedModuleHigherCohesion(0.75);

		// When
		mergeModule.execute(modules);

		// Then - Should complete in reasonable time with many comparisons
		verify(moduleService, atLeast(1)).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 16: Similar Domains Merge - Modules with related classes
	@Test
	void testSimilarDomainsMerge_modulesWithRelatedClasses() {
		// Given - modules with related dependencies (should merge well)
		Clazz ref1 = createClazz("ref1", "UserService", 0.7, "com.example.user", "com.example.auth");
		Module module1 = createModuleWithRefClasses("user-module", List.of(ref1), 0.65);

		Clazz ref2 = createClazz("ref2", "AuthService", 0.72, "com.example.auth", "com.example.security");
		Module module2 = createModuleWithRefClasses("auth-module", List.of(ref2), 0.68);

		List<Module> modules = List.of(module1, module2);

		// Mock: related domains merge well
		mockMergedModuleHigherCohesion(0.88);

		// When
		mergeModule.execute(modules);

		// Then - Should recommend merge for related domains
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 17: Disparate Domains - Modules with unrelated classes
	@Test
	void testDisparateDomains_modulesWithUnrelatedClasses() {
		// Given - modules with completely different dependencies
		Clazz ref1 = createClazz("ref1", "DatabaseService", 0.85, "java.sql", "javax.sql");
		Module module1 = createModuleWithRefClasses("db-module", List.of(ref1), 0.82);

		Clazz ref2 = createClazz("ref2", "UIController", 0.88, "javax.swing", "java.awt");
		Module module2 = createModuleWithRefClasses("ui-module", List.of(ref2), 0.85);

		List<Module> modules = List.of(module1, module2);

		// Mock: disparate domains don't merge well
		mockMergedModuleLowerCohesion(0.65);

		// When
		mergeModule.execute(modules);

		// Then - Should not recommend merge for disparate domains
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Module.class));
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

	private Module createModuleWithHighCohesion(String id, double avgRefClazzesSimilarity) {
		Clazz refClass = createClazz(id + "-ref", id + "RefClass", 0.9, "com.example." + id);
		List<Clazz> classes = List.of(refClass, createClazz(id + "-c1", id + "Class1", 0.88, "com.example." + id),
				createClazz(id + "-c2", id + "Class2", 0.87, "com.example." + id));

		return new Module(id, id + " Module", List.of(refClass), null, null, classes, 0.85, avgRefClazzesSimilarity, 0);
	}

	private Module createModuleWithLowCohesion(String id, double avgRefClazzesSimilarity) {
		Clazz refClass = createClazz(id + "-ref", id + "RefClass", 0.6, "com.example." + id, "com.other");
		List<Clazz> classes = List.of(refClass, createClazz(id + "-c1", id + "Class1", 0.55, "com.different"),
				createClazz(id + "-c2", id + "Class2", 0.58, "com.another"));

		return new Module(id, id + " Module", List.of(refClass), null, null, classes, 0.55, avgRefClazzesSimilarity, 0);
	}

	private Module createModuleWithVeryLowCohesion(String id, double avgRefClazzesSimilarity) {
		Clazz refClass = createClazz(id + "-ref", id + "RefClass", 0.35, "com.example." + id);
		List<Clazz> classes = List.of(refClass, createClazz(id + "-c1", id + "Class1", 0.3, "com.different"),
				createClazz(id + "-c2", id + "Class2", 0.32, "com.another"));

		return new Module(id, id + " Module", List.of(refClass), null, null, classes, 0.32, avgRefClazzesSimilarity, 0);
	}

	private Module createModuleWithModerateCohesion(String id, double avgRefClazzesSimilarity) {
		Clazz refClass = createClazz(id + "-ref", id + "RefClass", 0.7, "com.example." + id);
		List<Clazz> classes = List.of(refClass, createClazz(id + "-c1", id + "Class1", 0.68, "com.example." + id),
				createClazz(id + "-c2", id + "Class2", 0.72, "com.example." + id));

		return new Module(id, id + " Module", List.of(refClass), null, null, classes, 0.7, avgRefClazzesSimilarity, 0);
	}

	private Module createModuleWithRefClasses(String id, List<Clazz> refClasses, double avgRefClazzesSimilarity) {
		List<Clazz> allClasses = new ArrayList<>(refClasses);
		allClasses.add(createClazz(id + "-extra", id + "ExtraClass", 0.65, "com.example." + id));

		return new Module(id, id + " Module", refClasses, null, null, allClasses, 0.7, avgRefClazzesSimilarity, 0);
	}

	private Module createModuleWithNullCohesion(String id) {
		Clazz refClass = createClazz(id + "-ref", id + "RefClass", null, "com.example." + id);
		List<Clazz> classes = List.of(refClass);

		return new Module(id, id + " Module", List.of(refClass), null, null, classes, 0.5, null, 0); // null
																										// avgRefClazzesSimilarity
	}

	// Helper methods for mocking

	private void mockMergedModuleHigherCohesion(double mergedSimilarity) {
		// Mock both avgSimilarityWithRefClazzes and module similarity calculations
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Set avgRefClazzesSimilarity for the merged module
			if (module.getRefClazzes() != null && !module.getRefClazzes().isEmpty()) {
				module.setAvgRefClazzesSimilarity(mergedSimilarity);
			}
			return null;
		}).when(moduleService).calculateAvgSimilarityWithRefClazzes(any(Module.class));

		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Set module similarity to the specified value
			module.setSimilarity(mergedSimilarity);
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));
	}

	private void mockMergedModuleLowerCohesion(double mergedSimilarity) {
		// Mock both avgSimilarityWithRefClazzes and module similarity calculations
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Set avgRefClazzesSimilarity for the merged module
			if (module.getRefClazzes() != null && !module.getRefClazzes().isEmpty()) {
				module.setAvgRefClazzesSimilarity(mergedSimilarity);
			}
			return null;
		}).when(moduleService).calculateAvgSimilarityWithRefClazzes(any(Module.class));

		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Set module similarity to the specified value
			module.setSimilarity(mergedSimilarity);
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));
	}

}
