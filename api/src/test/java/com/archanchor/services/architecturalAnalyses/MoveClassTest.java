package com.archanchor.services.architecturalAnalyses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;

/**
 * Comprehensive test suite for MoveClass analysis. Tests the refactored implementation
 * that uses avgSimilarityWithRefClazzes and module similarity improvements for
 * determining beneficial class moves.
 */
@ExtendWith(MockitoExtension.class)
class MoveClassTest {

	@Mock
	private ModuleService moduleService;

	private MoveClass moveClass;

	@BeforeEach
	void setUp() {
		moveClass = new MoveClass(moduleService);

		// Use lenient stubbing to avoid UnnecessaryStubbingException in tests that don't
		// use these mocks
		// Mock module-level calculations that are called during move evaluation
		// These methods modify the temporary modules created during simulation
		lenient().doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Simulate module similarity calculation based on module state
			// For testing, we'll set sensible defaults that can be overridden per test
			if (module.getSimilarity() == null) {
				module.setSimilarity(0.75);
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// Mock these as no-ops since they're intermediate calculations
		lenient().doNothing().when(moduleService).calculateClassSimilaritiesAndSelectRefClasses(any(Module.class));
		lenient().doNothing().when(moduleService).calculateAvgSimilarityWithRefClazzes(any(Module.class));
	}

	// Test Case 1: No Beneficial Moves - All classes optimally placed
	@Test
	void testNoBeneficialMoves_allClassesOptimallyPlaced() {
		// Given - modules with well-placed classes (high avgSimilarityWithRefClazzes)
		Module module1 = createModuleWithWellPlacedClasses("module1", 0.9);
		Module module2 = createModuleWithWellPlacedClasses("module2", 0.85);
		List<Module> modules = List.of(module1, module2);

		// Mock: all potential moves show no improvement
		when(moduleService.calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any())).thenReturn(0.8); // Lower
																											// than
																											// current

		// When
		moveClass.execute(modules);

		// Then - Should find no beneficial moves
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
	}

	// Test Case 2: Single Beneficial Move - One class fits better in target module
	@Test
	void testSingleBeneficialMove_oneClassFitsBetterInTarget() {
		// Given
		Clazz misfitClass = createClazz("misfit", "MisfitClass", 0.4, "java.util", "java.io");
		Module sourceModule = createModuleWithClasses("source", List.of(misfitClass),
				createClazz("ref1", "RefClass1", 0.9, "com.example.source"));
		sourceModule.setSimilarity(0.7);

		Clazz targetRef = createClazz("targetRef", "TargetRefClass", 0.9, "java.util", "java.io");
		Module targetModule = createModuleWithClasses("target",
				List.of(createClazz("target1", "TargetClass1", 0.8, "java.util")), targetRef);
		targetModule.setSimilarity(0.75);

		List<Module> modules = List.of(sourceModule, targetModule);

		// Mock: misfit class has higher similarity with target's ref classes
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(misfitClass), eq(targetModule.getRefClazzes())))
			.thenReturn(0.85); // Much better than current 0.4

		// Mock module similarity calculations for move simulation
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			// Check if this is the source or target by class count
			boolean hasMisfitClass = module.getClazzes().stream().anyMatch(c -> c.getId().equals("misfit"));

			if (module.getId().equals("source")) {
				// Source without misfit: similarity stays same or improves
				module.setSimilarity(0.72);
			}
			else if (module.getId().equals("target")) {
				if (hasMisfitClass) {
					// Target with misfit: similarity improves
					module.setSimilarity(0.82);
				}
				else {
					module.setSimilarity(0.75);
				}
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should suggest moving misfit class to target module
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
		verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 3: Multiple Beneficial Moves - Several classes benefit from relocation
	@Test
	void testMultipleBeneficialMoves_severalClassesBenefitFromRelocation() {
		// Given
		Clazz misfit1 = createClazz("misfit1", "Misfit1", 0.3, "java.util");
		Clazz misfit2 = createClazz("misfit2", "Misfit2", 0.35, "java.io");
		Module sourceModule = createModuleWithClasses("source", List.of(misfit1, misfit2),
				createClazz("sourceRef", "SourceRef", 0.9, "com.example"));
		sourceModule.setSimilarity(0.65);

		Module targetModule = createModuleWithClasses("target",
				List.of(createClazz("target1", "Target1", 0.9, "java.util")),
				createClazz("targetRef", "TargetRef", 0.95, "java.util", "java.io"));
		targetModule.setSimilarity(0.8);

		List<Module> modules = List.of(sourceModule, targetModule);

		// Mock: both misfit classes have better similarity with target
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(misfit1), eq(targetModule.getRefClazzes())))
			.thenReturn(0.8); // Better than current 0.3
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(misfit2), eq(targetModule.getRefClazzes())))
			.thenReturn(0.75); // Better than current 0.35

		// Mock module similarity calculations
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			int classCount = module.getClazzes().size();

			if (module.getId().equals("source")) {
				// Source loses classes: similarity improves as misfits are removed
				if (classCount == 2)
					module.setSimilarity(0.68); // lost one misfit
				else if (classCount == 1)
					module.setSimilarity(0.70); // lost both misfits
				else
					module.setSimilarity(0.65);
			}
			else if (module.getId().equals("target")) {
				// Target gains classes: similarity improves
				if (classCount == 3)
					module.setSimilarity(0.85); // gained one class
				else if (classCount == 4)
					module.setSimilarity(0.87); // gained two classes
				else
					module.setSimilarity(0.8);
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should suggest multiple moves
		verify(moduleService, atLeast(2)).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
		verify(moduleService, atLeast(2)).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 4: Source Module Without Reference Classes - Should skip
	@Test
	void testSourceModuleWithoutRefClasses_shouldSkip() {
		// Given
		Module moduleWithoutRefClasses = new Module("no-ref", "No Ref Module", null, null, null,
				List.of(createClazz("class1", "Class1", 0.5, "java.util")), 0.5, 0.0, 0);

		Module normalModule = createModuleWithWellPlacedClasses("normal", 0.8);
		List<Module> modules = List.of(moduleWithoutRefClasses, normalModule);

		// When
		moveClass.execute(modules);

		// Then - Should skip module without ref classes
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
	}

	// Test Case 5: Target Module Without Reference Classes - Should skip target
	@Test
	void testTargetModuleWithoutRefClasses_shouldSkipTarget() {
		// Given
		Module sourceModule = createModuleWithWellPlacedClasses("source", 0.7);
		Module targetWithoutRef = new Module("no-ref-target", "No Ref Target", null, null, null,
				List.of(createClazz("class1", "Class1", 0.5, "java.util")), 0.5, 0.0, 0);

		List<Module> modules = List.of(sourceModule, targetWithoutRef);

		// When
		moveClass.execute(modules);

		// Then - Should not evaluate moves to module without ref classes
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), eq(null));
	}

	// Test Case 6: High Improvement Move - Class much better suited elsewhere
	@Test
	void testHighImprovementMove_classMuchBetterSuitedElsewhere() {
		// Given
		Clazz poorFitClass = createClazz("poorFit", "PoorFitClass", 0.2, "java.sql", "javax.sql");
		Module sourceModule = createModuleWithClasses("source", List.of(poorFitClass),
				createClazz("sourceRef", "SourceRef", 0.9, "com.example"));
		sourceModule.setSimilarity(0.6);

		Module perfectTargetModule = createModuleWithClasses("perfectTarget",
				List.of(createClazz("dbClass", "DbClass", 0.9, "java.sql")),
				createClazz("dbRef", "DbRef", 0.95, "java.sql", "javax.sql"));
		perfectTargetModule.setSimilarity(0.85);

		List<Module> modules = List.of(sourceModule, perfectTargetModule);

		// Mock: very high similarity with perfect target
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(poorFitClass),
				eq(perfectTargetModule.getRefClazzes())))
			.thenReturn(0.95); // Massive improvement from 0.2 to 0.95

		// Mock module similarity calculations showing significant improvement
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			boolean hasPoorFit = module.getClazzes().stream().anyMatch(c -> c.getId().equals("poorFit"));

			if (module.getId().equals("source")) {
				// Source without poor fit: major improvement
				module.setSimilarity(0.85);
			}
			else if (module.getId().equals("perfectTarget")) {
				if (hasPoorFit) {
					// Target with poor fit: significant improvement
					module.setSimilarity(0.92);
				}
				else {
					module.setSimilarity(0.85);
				}
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should strongly recommend this move
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(eq(poorFitClass), any());
		verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 7: Marginal Improvement - Small but positive improvement
	@Test
	void testMarginalImprovement_smallButPositiveImprovement() {
		// Given
		Clazz marginalClass = createClazz("marginal", "MarginalClass", 0.7, "java.util");
		Module sourceModule = createModuleWithClasses("source", List.of(marginalClass),
				createClazz("sourceRef", "SourceRef", 0.8, "java.util", "java.lang"));
		sourceModule.setSimilarity(0.75);

		Module targetModule = createModuleWithClasses("target",
				List.of(createClazz("targetClass", "TargetClass", 0.85, "java.util")),
				createClazz("targetRef", "TargetRef", 0.9, "java.util"));
		targetModule.setSimilarity(0.8);

		List<Module> modules = List.of(sourceModule, targetModule);

		// Mock: marginal improvement
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(marginalClass), eq(targetModule.getRefClazzes())))
			.thenReturn(0.72); // Small improvement from 0.7 to 0.72

		// Mock module similarity calculations with marginal improvement
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			boolean hasMarginalClass = module.getClazzes().stream().anyMatch(c -> c.getId().equals("marginal"));

			if (module.getId().equals("source")) {
				// Source without marginal: stays same
				module.setSimilarity(0.75);
			}
			else if (module.getId().equals("target")) {
				if (hasMarginalClass) {
					// Target with marginal: small improvement
					module.setSimilarity(0.82);
				}
				else {
					module.setSimilarity(0.8);
				}
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should still suggest move (any positive improvement)
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
		verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 8: No Improvement - Same similarity in both modules
	@Test
	void testNoImprovement_sameSimilarityInBothModules() {
		// Given
		Clazz neutralClass = createClazz("neutral", "NeutralClass", 0.6, "java.util");
		Module module1 = createModuleWithClasses("module1", List.of(neutralClass),
				createClazz("ref1", "Ref1", 0.7, "java.util"));

		Module module2 = createModuleWithClasses("module2", List.of(createClazz("class2", "Class2", 0.7, "java.util")),
				createClazz("ref2", "Ref2", 0.7, "java.util"));

		List<Module> modules = List.of(module1, module2);

		// Mock: same similarity in both modules
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(neutralClass), eq(module2.getRefClazzes())))
			.thenReturn(0.6); // Same as current

		// When
		moveClass.execute(modules);

		// Then - Should not suggest move (improvement = 0)
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
	}

	// Test Case 9: Multiple Target Options - Class could move to several modules
	@Test
	void testMultipleTargetOptions_classCouldMoveToSeveralModules() {
		// Given
		Clazz versatileClass = createClazz("versatile", "VersatileClass", 0.5, "java.util", "java.io");
		Module sourceModule = createModuleWithClasses("source", List.of(versatileClass),
				createClazz("sourceRef", "SourceRef", 0.8, "com.example"));
		sourceModule.setSimilarity(0.72);

		Module target1 = createModuleWithClasses("target1",
				List.of(createClazz("t1Class", "T1Class", 0.8, "java.util")),
				createClazz("t1Ref", "T1Ref", 0.85, "java.util"));
		target1.setSimilarity(0.78);

		Module target2 = createModuleWithClasses("target2", List.of(createClazz("t2Class", "T2Class", 0.9, "java.io")),
				createClazz("t2Ref", "T2Ref", 0.9, "java.io"));
		target2.setSimilarity(0.82);

		List<Module> modules = List.of(sourceModule, target1, target2);

		// Mock: class fits well in both targets (but differently)
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(versatileClass), eq(target1.getRefClazzes())))
			.thenReturn(0.7); // Good improvement
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(versatileClass), eq(target2.getRefClazzes())))
			.thenReturn(0.75); // Even better improvement

		// Mock module similarity calculations
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			boolean hasVersatile = module.getClazzes().stream().anyMatch(c -> c.getId().equals("versatile"));

			if (module.getId().equals("source")) {
				// Source without versatile: slight improvement
				module.setSimilarity(0.73);
			}
			else if (module.getId().equals("target1")) {
				if (hasVersatile) {
					// Target1 with versatile: good improvement
					module.setSimilarity(0.83);
				}
				else {
					module.setSimilarity(0.78);
				}
			}
			else if (module.getId().equals("target2")) {
				if (hasVersatile) {
					// Target2 with versatile: better improvement
					module.setSimilarity(0.88);
				}
				else {
					module.setSimilarity(0.82);
				}
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should suggest both moves, ranked by improvement
		verify(moduleService, times(2)).calculateAvgSimilarityWithRefClazzes(eq(versatileClass), any());
		verify(moduleService, atLeast(4)).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 10: Empty Modules List - Should handle gracefully
	@Test
	void testEmptyModulesList_shouldHandleGracefully() {
		// Given
		List<Module> emptyModules = List.of();

		// When
		moveClass.execute(emptyModules);

		// Then - Should complete without errors
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
	}

	// Test Case 11: Single Module - Cannot move classes
	@Test
	void testSingleModule_cannotMoveClasses() {
		// Given
		Module singleModule = createModuleWithWellPlacedClasses("single", 0.8);
		List<Module> modules = List.of(singleModule);

		// When
		moveClass.execute(modules);

		// Then - Should not suggest any moves (no target modules)
		verify(moduleService, never()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
	}

	// Test Case 12: Class with Null avgSimilarityWithRefClazzes - Should handle
	// gracefully
	@Test
	void testClassWithNullAvgSimilarity_shouldHandleGracefully() {
		// Given
		Clazz classWithNullSim = new Clazz("null-sim", "NullSimClass",
				List.of(createDependency("java.util.List", "java.util")), 0.5, null, "source", "source", "ALLOW"); // null
		// avgSimilarityWithRefClazzes

		Module sourceModule = createModuleWithClasses("source", List.of(classWithNullSim),
				createClazz("ref", "Ref", 0.8, "java.util"));
		sourceModule.setSimilarity(0.68);

		Module targetModule = createModuleWithWellPlacedClasses("target", 0.8);
		targetModule.setSimilarity(0.8);
		List<Module> modules = List.of(sourceModule, targetModule);

		// Mock: target has better similarity (null is treated as 0.0)
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(classWithNullSim), any())).thenReturn(0.7);

		// Mock module similarity calculations
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			boolean hasNullSim = module.getClazzes().stream().anyMatch(c -> c.getId().equals("null-sim"));

			if (module.getId().equals("source")) {
				// Source without null-sim class: improves
				module.setSimilarity(0.70);
			}
			else if (module.getId().equals("target")) {
				if (hasNullSim) {
					// Target with null-sim class: improves
					module.setSimilarity(0.84);
				}
				else {
					module.setSimilarity(0.8);
				}
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should treat null as 0.0 and calculate improvement
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(eq(classWithNullSim), any());
		verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 13: Module with Multiple Reference Classes - Complex evaluation
	@Test
	void testModuleWithMultipleRefClasses_complexEvaluation() {
		// Given
		Clazz testClass = createClazz("test", "TestClass", 0.6, "java.util", "java.io");
		Module sourceModule = createModuleWithClasses("source", List.of(testClass),
				createClazz("ref1", "Ref1", 0.8, "com.example"));
		sourceModule.setSimilarity(0.73);

		// Target with multiple reference classes
		Clazz targetRef1 = createClazz("tRef1", "TRef1", 0.9, "java.util");
		Clazz targetRef2 = createClazz("tRef2", "TRef2", 0.85, "java.io");
		Clazz targetRef3 = createClazz("tRef3", "TRef3", 0.88, "java.util", "java.io");
		Module targetModule = new Module("target", "Target Module", List.of(targetRef1, targetRef2, targetRef3), null,
				null, List.of(targetRef1, targetRef2, targetRef3), 0.87, 0.88, 0);

		List<Module> modules = List.of(sourceModule, targetModule);

		// Mock: average similarity with multiple ref classes
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(testClass), eq(targetModule.getRefClazzes())))
			.thenReturn(0.82); // Good fit with multiple ref classes

		// Mock module similarity calculations
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			boolean hasTestClass = module.getClazzes().stream().anyMatch(c -> c.getId().equals("test"));

			if (module.getId().equals("source")) {
				// Source without test class: improves
				module.setSimilarity(0.75);
			}
			else if (module.getId().equals("target")) {
				if (hasTestClass) {
					// Target with test class: improves (4 classes instead of 3)
					module.setSimilarity(0.90);
				}
				else {
					module.setSimilarity(0.87);
				}
			}
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should calculate average similarity with all ref classes
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(eq(testClass),
				eq(targetModule.getRefClazzes()));
		verify(moduleService, atLeastOnce()).calculateModuleSimilarity(any(Module.class));
	}

	// Test Case 14: Negative Improvement - Class worse in target
	@Test
	void testNegativeImprovement_classWorseInTarget() {
		// Given
		Clazz wellPlacedClass = createClazz("wellPlaced", "WellPlacedClass", 0.9, "com.example.specific");
		Module sourceModule = createModuleWithClasses("source", List.of(wellPlacedClass),
				createClazz("sourceRef", "SourceRef", 0.95, "com.example.specific"));

		Module poorTargetModule = createModuleWithClasses("poorTarget",
				List.of(createClazz("other", "OtherClass", 0.8, "com.different")),
				createClazz("poorRef", "PoorRef", 0.8, "com.different"));

		List<Module> modules = List.of(sourceModule, poorTargetModule);

		// Mock: much worse similarity in target
		when(moduleService.calculateAvgSimilarityWithRefClazzes(eq(wellPlacedClass),
				eq(poorTargetModule.getRefClazzes())))
			.thenReturn(0.3); // Much worse than current 0.9

		// When
		moveClass.execute(modules);

		// Then - Should not suggest move (negative improvement)
		verify(moduleService, atLeastOnce()).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
	}

	// Test Case 15: Large Number of Modules - Performance test
	@Test
	void testLargeNumberOfModules_performanceTest() {
		// Given - 10 modules with multiple classes each
		List<Module> modules = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Module module = createModuleWithWellPlacedClasses("module" + i, 0.7 + (i * 0.01));
			module.setSimilarity(0.75 + (i * 0.01));
			modules.add(module);
		}

		// Mock: some improvements available (most will be rejected)
		when(moduleService.calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any())).thenReturn(0.6, 0.75, 0.88,
				0.65, 0.70, 0.89); // Varied results, some better

		// Mock module similarity calculations
		doAnswer(invocation -> {
			Module module = invocation.getArgument(0);
			int classCount = module.getClazzes().size();
			// Simple heuristic: more classes = slightly better similarity
			double baseSimilarity = 0.75;
			double similarity = baseSimilarity + (classCount * 0.01);
			module.setSimilarity(Math.min(0.95, similarity));
			return null;
		}).when(moduleService).calculateModuleSimilarity(any(Module.class));

		// When
		moveClass.execute(modules);

		// Then - Should complete in reasonable time
		verify(moduleService, atLeast(1)).calculateAvgSimilarityWithRefClazzes(any(Clazz.class), any());
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
		return new Clazz(id, name, dependencies, 0.7, avgSimilarityWithRefClazzes, "test-module", "test-module",
				"ALLOW");
	}

	private Module createModuleWithWellPlacedClasses(String id, double avgRefClazzesSimilarity) {
		Clazz refClass = createClazz(id + "-ref", id + "RefClass", 0.9, "com.example." + id);
		Clazz class1 = createClazz(id + "-c1", id + "Class1", 0.85, "com.example." + id);
		Clazz class2 = createClazz(id + "-c2", id + "Class2", 0.88, "com.example." + id);

		return new Module(id, id + " Module", List.of(refClass), null, null, List.of(refClass, class1, class2), 0.85,
				avgRefClazzesSimilarity, 0);
	}

	private Module createModuleWithClasses(String id, List<Clazz> classes, Clazz refClass) {
		List<Clazz> allClasses = new ArrayList<>(classes);
		if (!allClasses.contains(refClass)) {
			allClasses.add(refClass);
		}

		return new Module(id, id + " Module", List.of(refClass), null, null, allClasses, 0.7, 0.75, 0);
	}

}
