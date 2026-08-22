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

	private List<SplitModuleResult> results;

	public SplitModule(ModuleService moduleService) {
		super(moduleService);
	}

	@Override
	public void execute(List<Module> modules) {
		this.results = new ArrayList<>();
		splitModuleAnalysis(modules);
	}

	public List<SplitModuleResult> getResults() {
		return results != null ? new ArrayList<>(results) : new ArrayList<>();
	}

	public static class SplitModuleResult {

		public String originalModuleId;

		public String originalModuleName;

		public String module1Id;

		public String module1Name;

		public String module2Id;

		public String module2Name;

		public String superRefClassName;

		public String deRefClassName;

		public List<String> module1ClassNames;

		public List<String> module2ClassNames;

		public double originalSimilarity;

		public double module1Similarity;

		public double module2Similarity;

		public int originalViolations;

		public int module1Violations;

		public int module2Violations;

		public double similarityImprovement;

		public double violationsImprovement;

		public double rate;

		public SplitModuleResult(String originalModuleId, String originalModuleName, String module1Id,
				String module1Name, String module2Id, String module2Name, String superRefClassName,
				String deRefClassName, List<String> module1ClassNames, List<String> module2ClassNames,
				double originalSimilarity, double module1Similarity, double module2Similarity, int originalViolations,
				int module1Violations, int module2Violations, double similarityImprovement,
				double violationsImprovement, double rate) {
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
			this.originalViolations = originalViolations;
			this.module1Violations = module1Violations;
			this.module2Violations = module2Violations;
			this.similarityImprovement = similarityImprovement;
			this.violationsImprovement = violationsImprovement;
			this.rate = rate;
		}

	}

	/**
	 * Finds the reference class with the highest avgSimilarityWithRefClazzes
	 * (superRefClass)
	 */
	private Clazz findSuperRefClass(Module module) {
		if (module.getRefClazzes() == null || module.getRefClazzes().isEmpty()) {
			return null;
		}

		return module.getRefClazzes().stream().max((c1, c2) -> {
			double sim1 = c1.getAvgSimilarityWithRefClazzes() != null ? c1.getAvgSimilarityWithRefClazzes() : 0.0;
			double sim2 = c2.getAvgSimilarityWithRefClazzes() != null ? c2.getAvgSimilarityWithRefClazzes() : 0.0;
			return Double.compare(sim1, sim2);
		}).orElse(null);
	}

	/**
	 * Finds the non-reference class with the lowest avgSimilarityWithRefClazzes
	 * (deRefClass)
	 */
	private Clazz findDeRefClass(Module module) {
		List<Clazz> refClazzes = module.getRefClazzes() != null ? module.getRefClazzes() : new ArrayList<>();

		return module.getClazzes()
			.stream()
			.filter(clazz -> !refClazzes.contains(clazz)) // Exclude reference classes
			.min((c1, c2) -> {
				double sim1 = c1.getAvgSimilarityWithRefClazzes() != null ? c1.getAvgSimilarityWithRefClazzes() : 0.0;
				double sim2 = c2.getAvgSimilarityWithRefClazzes() != null ? c2.getAvgSimilarityWithRefClazzes() : 0.0;
				return Double.compare(sim1, sim2);
			})
			.orElse(null);
	}

	/**
	 * Assigns each class to either module1 (superRefClass side) or module2 (deRefClass
	 * side) based on which edge it is closer to
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
			}
			else {
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

		return new Module(newId, newName, List.of(refClass), // Set the edge class as
																// reference
				null, null, new ArrayList<>(classes), 0.0, 0.0, 0 // violations will be
																	// calculated later
		);
	}

	/**
	 * Calculates the rate for splitting a module by considering both similarity and
	 * violations improvements. Uses normalized deltas for similarity and violations,
	 * weighted equally.
	 * @return A SplitRateResult containing the rate and detailed metrics (rate can be
	 * positive or negative)
	 */
	private SplitRateResult calculateSplitRate(Module original, Module module1, Module module2) {
		// Calculate all similarities for module1
		this.moduleService.calculateClassSimilaritiesAndSelectRefClasses(module1);
		this.moduleService.calculateAvgSimilarityWithRefClazzes(module1);
		this.moduleService.calculateModuleSimilarity(module1);
		this.moduleService.populateRefClazzesDependencies(module1);
		this.moduleService.populateModuleDependencies(module1);
		this.moduleService.calculateModuleViolations(module1);

		// Calculate all similarities for module2
		this.moduleService.calculateClassSimilaritiesAndSelectRefClasses(module2);
		this.moduleService.calculateAvgSimilarityWithRefClazzes(module2);
		this.moduleService.calculateModuleSimilarity(module2);
		this.moduleService.populateRefClazzesDependencies(module2);
		this.moduleService.populateModuleDependencies(module2);
		this.moduleService.calculateModuleViolations(module2);

		// Get similarity values
		double originalSimilarity = original.getSimilarity() != null ? original.getSimilarity() : 0.0;
		double module1Similarity = module1.getSimilarity() != null ? module1.getSimilarity() : 0.0;
		double module2Similarity = module2.getSimilarity() != null ? module2.getSimilarity() : 0.0;

		// Calculate similarity improvement (already normalized as similarity is [0, 1])
		double avgSplitSimilarity = (module1Similarity + module2Similarity) / 2.0;
		double similarityImprovement = avgSplitSimilarity - originalSimilarity;

		// Get violations values
		int originalViolations = original.getViolations() != null ? original.getViolations() : 0;
		int module1Violations = module1.getViolations() != null ? module1.getViolations() : 0;
		int module2Violations = module2.getViolations() != null ? module2.getViolations() : 0;

		// Calculate violations improvement (normalize by before state)
		int totalSplitViolations = module1Violations + module2Violations;
		double violationsReduction = originalViolations - totalSplitViolations;
		double normalizedViolationsImprovement = violationsReduction / Math.max(originalViolations, 1.0);

		// Calculate rate using weighted combination
		double rate = (ModuleService.SIMILARITY_WEIGHT * similarityImprovement)
				+ (ModuleService.VIOLATION_WEIGHT * normalizedViolationsImprovement);

		return new SplitRateResult(originalViolations, module1Violations, module2Violations, similarityImprovement,
				normalizedViolationsImprovement, rate);
	}

	private static class SplitRateResult {

		int originalViolations;

		int module1Violations;

		int module2Violations;

		double similarityImprovement;

		double violationsImprovement;

		double rate;

		public SplitRateResult(int originalViolations, int module1Violations, int module2Violations,
				double similarityImprovement, double violationsImprovement, double rate) {
			this.originalViolations = originalViolations;
			this.module1Violations = module1Violations;
			this.module2Violations = module2Violations;
			this.similarityImprovement = similarityImprovement;
			this.violationsImprovement = violationsImprovement;
			this.rate = rate;
		}

	}

	public void splitModuleAnalysis(List<Module> modules) {
		log.info("Starting edge-based split module analysis for {} modules...", modules.size());

		for (Module module : modules) {
			// Skip modules without reference classes
			if (module.getRefClazzes() == null || module.getRefClazzes().isEmpty()) {
				log.debug("Skipping module '{}' - no reference classes defined", module.getName());
				continue;
			}

			// Need at least 3 classes: 1 reference class, 1 non-reference class to form
			// edges, and 1 to split
			if (module.getClazzes().size() < 3) {
				log.debug("Skipping module '{}' - insufficient classes for splitting ({})", module.getName(),
						module.getClazzes().size());
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

			// Calculate split rate
			SplitRateResult rateResult = calculateSplitRate(module, module1, module2);

			// Only recommend split if rate is positive (considering both similarity and
			// violations)
			if (rateResult.rate > 0) {
				List<String> module1ClassNames = module1Classes.stream()
					.map(Clazz::getName)
					.collect(Collectors.toList());

				List<String> module2ClassNames = module2Classes.stream()
					.map(Clazz::getName)
					.collect(Collectors.toList());

				results.add(new SplitModuleResult(module.getId(), module.getName(), module1.getId(), module1.getName(),
						module2.getId(), module2.getName(), superRefClass.getName(), deRefClass.getName(),
						module1ClassNames, module2ClassNames, module.getSimilarity(), module1.getSimilarity(),
						module2.getSimilarity(), rateResult.originalViolations, rateResult.module1Violations,
						rateResult.module2Violations, rateResult.similarityImprovement,
						rateResult.violationsImprovement, rateResult.rate));

				log.debug(
						"Beneficial split found for '{}': Rate: {}, Similarity improvement: {}, Violations improvement: {}",
						module.getName(), rateResult.rate, rateResult.similarityImprovement,
						rateResult.violationsImprovement);
			}
		}

		results.sort((a, b) -> Double.compare(b.rate, a.rate));

		log.info("Found {} potential beneficial splits", results.size());
		if (results.isEmpty()) {
			log.info("No beneficial split suggestions found. All modules appear to have optimal cohesion.");
		}
		else {
			log.info("Top split suggestions:");
			for (int i = 0; i < Math.min(5, results.size()); i++) {
				SplitModuleResult split = results.get(i);
				log.info("  {}. Split module '{}'", (i + 1), split.originalModuleName);
				log.info("     Original similarity: {}", String.format("%.4f", split.originalSimilarity));
				log.info("     → Module 1 '{}' (ref: '{}'): {} classes, similarity: {}", split.module1Name,
						split.superRefClassName, split.module1ClassNames.size(),
						String.format("%.4f", split.module1Similarity));
				log.info("     → Module 2 '{}' (ref: '{}'): {} classes, similarity: {}", split.module2Name,
						split.deRefClassName, split.module2ClassNames.size(),
						String.format("%.4f", split.module2Similarity));
				log.info("     Rate: +{} (Similarity: {}, Violations: {} → {})", String.format("%.4f", split.rate),
						String.format("%.4f", split.similarityImprovement), split.originalViolations,
						(split.module1Violations + split.module2Violations));
			}
		}
	}

}