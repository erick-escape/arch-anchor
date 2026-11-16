package com.archanchor.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.module.Module;
import com.archanchor.dto.clazz.ClazzResponseDTO;
import com.archanchor.dto.dependencies.DependencyDTO;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.mapstruct.*;
import com.archanchor.services.parsers.LanguageParsers;
import com.archanchor.services.parsers.ParsedClazz;
import com.archanchor.services.parsers.SourceFileOrder;
import com.archanchor.services.rating.ActiveRatingWeights;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ModuleService {

	private final LanguageParsers languageParsers;

	private final ModuleMapper moduleMapper = new ModuleMapperImpl();

	private final ClazzMapper clazzMapper = new ClazzMapperImpl();

	private final DependencyMapper dependencyMapper = new DependencyMapperImpl();

	private final ActiveRatingWeights activeWeights;

	public ModuleService(LanguageParsers languageParsers, ActiveRatingWeights activeWeights) {
		this.languageParsers = languageParsers;
		this.activeWeights = activeWeights;
	}

	public void saveModules(List<ModuleDTO> modulesList) {
		if (modulesList == null) {
			throw new IllegalArgumentException("Modules list cannot be null");
		}

		File file = new File("modules.bin");
		try (ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(file))) {
			objectOutput.writeObject(modulesList);
		}
		catch (IOException e) {
			throw new RuntimeException("Error saving modules to file: " + e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public List<ModuleDTO> getModulesFromFile() {
		File file = new File("modules.bin");

		if (!file.exists() || file.length() == 0) {
			return new ArrayList<>();
		}

		try (ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(file))) {
			return (List<ModuleDTO>) objectInput.readObject();
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException("Error reading modules: Class not found", e);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading modules from file: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns the saved modules in the shape the analyses need. Reference classes are the
	 * same instances as in the class list, because the analyses tell classes apart by
	 * identity; violations are recomputed, because ModuleDTO does not carry them (#19).
	 *
	 * <pre>
	 * analysesRunner.executeAll(moduleService.savedModulesForAnalysis());
	 * </pre>
	 */
	public List<Module> savedModulesForAnalysis() {
		List<Module> savedModules = getModulesFromFile().stream()
			.map(moduleMapper::toEntity)
			.collect(Collectors.toList());
		savedModules.forEach(this::relinkReferenceClasses);
		savedModules.forEach(this::calculateModuleViolations);
		return savedModules;
	}

	/**
	 * Points the module's reference classes at the instances in its class list, matched
	 * by id. Mapping from the DTO builds refClazzes and clazzes separately, so a
	 * reference class comes back as two objects, and the analyses compare classes by
	 * identity. A reference class missing from the class list is left as it is: nothing
	 * in this class produces one.
	 *
	 * <pre>
	 * moduleService.relinkReferenceClasses(moduleMapper.toEntity(savedModule));
	 * </pre>
	 */
	public void relinkReferenceClasses(Module module) {
		if (module.getRefClazzes() == null || module.getClazzes() == null) {
			return;
		}
		Map<String, Clazz> clazzesById = new HashMap<>();
		module.getClazzes().forEach(clazz -> clazzesById.putIfAbsent(clazz.getId(), clazz));
		module.setRefClazzes(module.getRefClazzes()
			.stream()
			.map(refClazz -> clazzesById.getOrDefault(refClazz.getId(), refClazz))
			.collect(Collectors.toList()));
	}

	public void saveProjectAnalyses(ProjectAnalysesDTO projectAnalyses) {
		File file = new File("project-analyses.bin");
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
			out.writeObject(projectAnalyses);
		}
		catch (IOException e) {
			throw new RuntimeException("Error saving project analyses: " + e.getMessage(), e);
		}
	}

	public ProjectAnalysesDTO getProjectAnalysesFromFile() {
		File file = new File("project-analyses.bin");
		if (!file.exists() || file.length() == 0)
			return null;
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
			return (ProjectAnalysesDTO) in.readObject();
		}
		catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException("Error reading project analyses: " + e.getMessage(), e);
		}
	}

	public String generateNewUUID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Generates a deterministic UUID based on a string input to prevent duplicate modules
	 */
	private String generateDeterministicUUID(String input) {
		try {
			// Use a hash of the input string to generate a consistent UUID
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

			// Convert the hash bytes to a UUID format
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}

			String hashString = sb.toString();
			// Format as UUID: 8-4-4-4-12
			return String.format("%s-%s-%s-%s-%s", hashString.substring(0, 8), hashString.substring(8, 12),
					hashString.substring(12, 16), hashString.substring(16, 20), hashString.substring(20, 32));
		}
		catch (Exception e) {
			// Fallback to random UUID if hashing fails
			return UUID.randomUUID().toString();
		}
	}

	/**
	 * Calculates the number of violations if the given class were to be selected as
	 * refClass. Violations are dependencies present in module dependencies but not in the
	 * candidate class's dependencies.
	 * @param candidateClass The class being evaluated as a potential refClass
	 * @param moduleDependencies All dependencies from all classes in the module
	 * @return The number of violations (dependencies that would not be allowed)
	 */
	private int calculateViolations(Clazz candidateClass, List<Dependency> moduleDependencies, ModuleScope scope) {
		if (moduleDependencies == null || moduleDependencies.isEmpty()) {
			return 0;
		}

		// Get the dependency package names from the candidate class, leaving out its
		// references to the module's own classes (#43)
		Set<String> candidateDependencies = scope.packagesOf(candidateClass);

		// Count how many module dependencies are NOT in the candidate's dependencies
		// These would be violations if this class became the refClass
		return (int) moduleDependencies.stream()
			.map(Dependency::getPackageName)
			.filter(packageName -> !candidateDependencies.contains(packageName))
			.count();
	}

	/**
	 * Calculates pairwise similarity between all classes within a module. Sets the
	 * similarity field on each Clazz object. Does NOT modify the module's reference
	 * classes.
	 * @param module The module containing classes to calculate similarities for
	 */
	public void calculateClassSimilarities(Module module) {
		List<Clazz> clazzes = module.getClazzes();
		ModuleScope scope = ModuleScope.of(module);

		if (clazzes.size() > 1) {
			for (Clazz clazz : clazzes) {
				double totalSimilarity = 0.0;
				for (Clazz otherClazz : clazzes) {
					if (!clazz.equals(otherClazz)) {
						totalSimilarity += calculateSimilarity(clazz, otherClazz, scope);
					}
				}
				clazz.setSimilarity(totalSimilarity / (clazzes.size() - 1));
			}
		}
		else if (clazzes.size() == 1) {
			Clazz clazz = clazzes.get(0);
			clazz.setSimilarity(1.0);
		}
	}

	/**
	 * Selects and sets reference classes for a module based on weighted rating. Uses a
	 * combination of similarity and violations to select the best reference class.
	 * Requires that class similarities have been previously calculated.
	 * @param module The module to select reference classes for
	 */
	public void selectReferenceClasses(Module module) {
		List<Clazz> clazzes = module.getClazzes();

		if (clazzes.isEmpty()) {
			return;
		}

		// Ensure module dependencies are populated for violation calculation
		populateModuleDependencies(module);
		List<Dependency> moduleDependencies = module.getModuleDependencies();
		ModuleScope scope = ModuleScope.of(module);

		// Calculate violations for each class and find max for normalization
		Map<Clazz, Integer> violationsMap = new HashMap<>();
		int maxViolations = 0;

		for (Clazz clazz : clazzes) {
			int violations = calculateViolations(clazz, moduleDependencies, scope);
			violationsMap.put(clazz, violations);
			maxViolations = Math.max(maxViolations, violations);
		}

		// Find max similarity for normalization
		double maxSimilarity = clazzes.stream().mapToDouble(Clazz::getSimilarity).max().orElse(1.0);

		// Prevent division by zero
		if (maxSimilarity == 0.0) {
			maxSimilarity = 1.0;
		}

		// Select refClazz based on weighted rate. Stream.max keeps the first of equally
		// rated classes, and getClazzes lists classes by file name, so in a module read
		// from disk a tie goes to the alphabetically first class (#15).
		Clazz refClazz;
		if (maxViolations == 0) {
			// If no violations exist, select based on similarity only
			refClazz = clazzes.stream().max(Comparator.comparingDouble(Clazz::getSimilarity)).orElse(null);
			log.info("Selected refClass based on similarity only (no violations): {}",
					refClazz != null ? refClazz.getName() : "null");
		}
		else {
			// Calculate weighted rate for each class
			final int finalMaxViolations = maxViolations;
			final double finalMaxSimilarity = maxSimilarity;

			refClazz = clazzes.stream().max(Comparator.comparingDouble(clazz -> {
				double similarity = clazz.getSimilarity();
				int violations = violationsMap.get(clazz);

				// Normalize similarity to [0, 1] where 1 is best (highest similarity)
				double normalizedSimilarity = similarity / finalMaxSimilarity;

				// Normalize violations to [0, 1] where 1 is best (no violations)
				double normalizedViolations = 1.0 - ((double) violations / finalMaxViolations);

				// Combined with the project's election weights (#27): higher is better
				double rate = activeWeights.current().election().rate(normalizedSimilarity, normalizedViolations);

				log.debug(
						"Class: {}, Similarity: {}, Normalized Similarity: {}, Violations: {}, Normalized Violations: {}, Rate: {}",
						clazz.getName(), similarity, normalizedSimilarity, violations, normalizedViolations, rate);
				return rate;
			})).orElse(null);

			if (refClazz != null) {
				log.info("Selected refClass: {} with similarity: {} and violations: {}", refClazz.getName(),
						refClazz.getSimilarity(), violationsMap.get(refClazz));
			}
		}

		List<Clazz> refClazzes = List.of(refClazz);
		module.setRefClazzes(refClazzes);
		module.setRefClazzesChosenByHand(false);
	}

	/**
	 * Convenience method that calculates class similarities and then selects reference
	 * classes. This provides the same behavior as the original calculateClassSimilarities
	 * method.
	 * @param module The module to process
	 */
	/**
	 * Measures a module with the reference classes it already has, without an election. A
	 * merged module keeps the reference classes of both modules: the merge card is rated
	 * on that module, and applying the merge builds the same one (#34).
	 *
	 * <pre>
	 * merged.setRefClazzes(bothModulesReferenceClasses);
	 * moduleService.measureWithReferenceClasses(merged);
	 * </pre>
	 */
	public void measureWithReferenceClasses(Module module) {
		calculateClassSimilarities(module);
		calculateAvgSimilarityWithRefClazzes(module);
		calculateModuleSimilarity(module);
		populateRefClazzesDependencies(module);
		populateModuleDependencies(module);
		calculateModuleViolations(module);
	}

	public void calculateClassSimilaritiesAndSelectRefClasses(Module module) {
		calculateClassSimilarities(module);
		selectReferenceClasses(module);
	}

	public void calculateAvgSimilarityWithRefClazzes(Module module) {
		List<Clazz> clazzes = module.getClazzes();
		List<Clazz> refClazzes = module.getRefClazzes();
		ModuleScope scope = ModuleScope.of(module);

		if (refClazzes == null || refClazzes.isEmpty()) {
			// If no reference classes, set all avgSimilarityWithRefClazzes to 0.0
			for (Clazz clazz : clazzes) {
				clazz.setAvgSimilarityWithRefClazzes(0.0);
			}
			return;
		}

		if (refClazzes.size() == 1) {
			// If only one reference class, set its avgSimilarityWithRefClazzes to 1.0
			Clazz refClazz = refClazzes.get(0);
			refClazz.setAvgSimilarityWithRefClazzes(1.0);

			// Calculate avgSimilarityWithRefClazzes for other classes with this unique
			// reference class
			for (Clazz clazz : clazzes) {
				if (!clazz.equals(refClazz)) {
					double similarity = calculateSimilarity(clazz, refClazz, scope);
					clazz.setAvgSimilarityWithRefClazzes(similarity);
				}
			}
		}
		else {
			// Multiple reference classes
			for (Clazz clazz : clazzes) {
				double totalSimilarity = 0.0;
				int count = 0;

				if (refClazzes.contains(clazz)) {
					// If this class is a reference class, calculate with other reference
					// classes
					for (Clazz refClazz : refClazzes) {
						if (!clazz.equals(refClazz)) {
							totalSimilarity += calculateSimilarity(clazz, refClazz, scope);
							count++;
						}
					}
				}
				else {
					// If not a reference class, calculate with all reference classes
					for (Clazz refClazz : refClazzes) {
						totalSimilarity += calculateSimilarity(clazz, refClazz, scope);
						count++;
					}
				}

				clazz.setAvgSimilarityWithRefClazzes(count > 0 ? totalSimilarity / count : 0.0);
			}
		}
	}

	public void calculateModuleSimilarity(Module module) {
		List<Clazz> clazzes = module.getClazzes();

		if (!clazzes.isEmpty()) {
			double totalSimilarity = 0.0;
			for (Clazz clazz : clazzes) {
				totalSimilarity += clazz.getSimilarity();
			}
			module.setSimilarity(totalSimilarity / clazzes.size());
		}
		else {
			module.setSimilarity(0.0);
		}

		// Calculate avgRefClazzesSimilarity
		List<Clazz> refClazzes = module.getRefClazzes();
		if (refClazzes != null && !refClazzes.isEmpty()) {
			double totalRefSimilarity = 0.0;
			for (Clazz refClazz : refClazzes) {
				if (refClazz.getAvgSimilarityWithRefClazzes() != null) {
					totalRefSimilarity += refClazz.getAvgSimilarityWithRefClazzes();
				}
			}
			module.setAvgRefClazzesSimilarity(totalRefSimilarity / refClazzes.size());
		}
		else {
			module.setAvgRefClazzesSimilarity(0.0);
		}
	}

	/**
	 * Calculates the average similarity of a single class with a list of reference
	 * classes. This method is useful for evaluating how well a class would fit in a
	 * module based on its similarity with the module's reference classes.
	 * @param clazz The class to calculate similarity for
	 * @param refClazzes The reference classes to compare against
	 * @return The average similarity with reference classes, or 0.0 if no reference
	 * classes
	 */
	public double calculateAvgSimilarityWithRefClazzes(Clazz clazz, List<Clazz> refClazzes) {
		return calculateAvgSimilarityWithRefClazzes(clazz, refClazzes, ModuleScope.of(List.of()));
	}

	/**
	 * The same, for a class in the module that {@code scope} describes, e.g. a target
	 * with the class moved in (#43).
	 */
	public double calculateAvgSimilarityWithRefClazzes(Clazz clazz, List<Clazz> refClazzes, ModuleScope scope) {
		if (refClazzes == null || refClazzes.isEmpty()) {
			return 0.0;
		}

		if (refClazzes.size() == 1) {
			return calculateSimilarity(clazz, refClazzes.get(0), scope);
		}

		double totalSimilarity = 0.0;
		for (Clazz refClazz : refClazzes) {
			totalSimilarity += calculateSimilarity(clazz, refClazz, scope);
		}

		return totalSimilarity / refClazzes.size();
	}

	public double calculateSimilarity(Clazz clazz1, Clazz clazz2) {
		return calculateSimilarity(clazz1, clazz2, ModuleScope.of(List.of()));
	}

	/**
	 * The similarity of two classes of one module, leaving out their references to the
	 * module's own classes (#43).
	 */
	public double calculateSimilarity(Clazz clazz1, Clazz clazz2, ModuleScope scope) {
		Set<String> deps1 = scope.packagesOf(clazz1);
		Set<String> deps2 = scope.packagesOf(clazz2);

		int a = (int) deps1.stream().filter(deps2::contains).count();
		int b = deps1.size() - a;
		int c = deps2.size() - a;
		int firstDenominator = (a + b) == 0 ? 1 : (a + b);
		int secondDenominator = (a + c) == 0 ? 1 : (a + c);

		return 0.5 * (((double) a / firstDenominator) + ((double) a / secondDenominator));
	}

	/**
	 * Calculates the number of violations in a module. Violations are dependencies used
	 * by classes in the module that are NOT in the allowedRules (i.e., dependencies not
	 * present in refClazzes).
	 * @param module The module to calculate violations for
	 */
	public void calculateModuleViolations(Module module) {
		if (module == null) {
			return;
		}

		// If no refClazzes or no refClazzesDependencies, set violations to 0
		if (module.getRefClazzes() == null || module.getRefClazzes().isEmpty()
				|| module.getRefClazzesDependencies() == null || module.getRefClazzesDependencies().isEmpty()) {
			module.setViolations(0);
			return;
		}

		// If no module dependencies, set violations to 0
		if (module.getModuleDependencies() == null || module.getModuleDependencies().isEmpty()) {
			module.setViolations(0);
			return;
		}

		// Get allowed rules (refClazzes dependencies)
		Set<String> allowedRules = module.getRefClazzesDependencies()
			.stream()
			.map(Dependency::getPackageName)
			.filter(packageName -> packageName != null && !packageName.isEmpty())
			.collect(Collectors.toSet());

		// Count violations: module dependencies NOT in allowed rules
		int violations = (int) module.getModuleDependencies()
			.stream()
			.map(Dependency::getPackageName)
			.filter(packageName -> packageName != null && !packageName.isEmpty())
			.filter(packageName -> !allowedRules.contains(packageName))
			.count();

		module.setViolations(violations);
	}

	public void populateModuleDependencies(Module module) {
		if (module.getClazzes() == null || module.getClazzes().isEmpty()) {
			module.setModuleDependencies(new ArrayList<>());
			return;
		}

		Map<String, Dependency> uniqueDependencies = new HashMap<>();
		// References to the module's own classes are internal (#43)
		ModuleScope scope = ModuleScope.of(module);

		for (Clazz clazz : module.getClazzes()) {
			if (clazz.getDependencies() != null) {
				for (Dependency dependency : scope.dependenciesOf(clazz)) {
					if (dependency.getPackageName() != null && !dependency.getPackageName().isEmpty()) {
						String packageName = dependency.getPackageName();

						if (uniqueDependencies.containsKey(packageName)) {
							// Merge types into the aggregated dependency with
							// deduplication
							Dependency existingDependency = uniqueDependencies.get(packageName);
							if (dependency.getTypes() != null) {
								for (var type : dependency.getTypes()) {
									boolean typeExists = existingDependency.getTypes()
										.stream()
										.anyMatch(t -> t.getFullyQualifiedName().equals(type.getFullyQualifiedName()));
									if (!typeExists) {
										existingDependency.addType(type);
									}
								}
							}
						}
						else {
							// Create a new Dependency copy so we never mutate the class's
							// own object
							Dependency copy = new Dependency(packageName);
							if (dependency.getTypes() != null) {
								for (var type : dependency.getTypes()) {
									copy.addType(type);
								}
							}
							uniqueDependencies.put(packageName, copy);
						}
					}
				}
			}
		}

		List<Dependency> allDependencies = new ArrayList<>(uniqueDependencies.values());
		module.setModuleDependencies(allDependencies);
	}

	public void populateRefClazzesDependencies(Module module) {
		if (module.getRefClazzes() == null || module.getRefClazzes().isEmpty()) {
			module.setRefClazzesDependencies(new ArrayList<>());
			return;
		}

		Map<String, Dependency> uniqueDependencies = new HashMap<>();
		// A reference class's references to the module's own classes allow nothing (#43)
		ModuleScope scope = ModuleScope.of(module);

		for (Clazz clazz : module.getRefClazzes()) {
			if (clazz.getDependencies() != null) {
				for (Dependency dependency : scope.dependenciesOf(clazz)) {
					if (dependency.getPackageName() != null && !dependency.getPackageName().isEmpty()) {
						String packageName = dependency.getPackageName();

						if (uniqueDependencies.containsKey(packageName)) {
							// Merge types into the aggregated dependency with
							// deduplication
							Dependency existingDependency = uniqueDependencies.get(packageName);
							if (dependency.getTypes() != null) {
								for (var type : dependency.getTypes()) {
									boolean typeExists = existingDependency.getTypes()
										.stream()
										.anyMatch(t -> t.getFullyQualifiedName().equals(type.getFullyQualifiedName()));
									if (!typeExists) {
										existingDependency.addType(type);
									}
								}
							}
						}
						else {
							// Create a new Dependency copy so we never mutate the class's
							// own object
							Dependency copy = new Dependency(packageName);
							if (dependency.getTypes() != null) {
								for (var type : dependency.getTypes()) {
									copy.addType(type);
								}
							}
							uniqueDependencies.put(packageName, copy);
						}
					}
				}
			}
		}

		List<Dependency> refClazzesDependencies = new ArrayList<>(uniqueDependencies.values());
		module.setRefClazzesDependencies(refClazzesDependencies);
	}

	public List<ModuleDTO> splitModule(ModuleDTO originalModule, List<String> classIds) {
		// Convert to entity for easier manipulation
		Module originalModuleEntity = moduleMapper.toEntity(originalModule);

		// Partition classes
		Map<Boolean, List<ClazzResponseDTO>> partitionedClasses = Arrays.stream(originalModule.clazzes())
			.collect(Collectors.partitioningBy(clazz -> classIds.contains(clazz.id())));

		List<ClazzResponseDTO> retainedClasses = partitionedClasses.get(false);
		List<ClazzResponseDTO> extractedClasses = partitionedClasses.get(true);

		// Create the retained module (original with fewer classes)
		Module retainedModuleEntity = new Module();
		retainedModuleEntity.setId(originalModuleEntity.getId());
		retainedModuleEntity.setName(originalModuleEntity.getName() + "_Retained");
		retainedModuleEntity.setClazzes(retainedClasses.stream().map(clazzMapper::toEntity).toList());

		this.calculateClassSimilaritiesAndSelectRefClasses(retainedModuleEntity);
		this.calculateAvgSimilarityWithRefClazzes(retainedModuleEntity);
		this.calculateModuleSimilarity(retainedModuleEntity);
		this.populateRefClazzesDependencies(retainedModuleEntity);
		this.populateModuleDependencies(retainedModuleEntity);
		this.calculateModuleViolations(retainedModuleEntity);

		// Create the new module (with extracted classes)
		Module newModuleEntity = new Module();
		newModuleEntity.setId(generateNewUUID());
		newModuleEntity.setName(originalModuleEntity.getName() + "_Splited");
		newModuleEntity.setClazzes(extractedClasses.stream().map(clazzMapper::toEntity).toList());

		this.calculateClassSimilaritiesAndSelectRefClasses(newModuleEntity);
		this.calculateAvgSimilarityWithRefClazzes(newModuleEntity);
		this.calculateModuleSimilarity(newModuleEntity);
		this.populateRefClazzesDependencies(newModuleEntity);
		this.populateModuleDependencies(newModuleEntity);
		this.calculateModuleViolations(newModuleEntity);

		// Convert back to DTOs
		ModuleDTO retainedDto = moduleMapper.toDto(retainedModuleEntity);
		ModuleDTO newDto = moduleMapper.toDto(newModuleEntity);

		return Arrays.asList(retainedDto, newDto);
	}

	/**
	 * The project's modules as its sources lay them out: one for every directory that
	 * holds a class, whatever language the classes are written in (#81). Modules are
	 * ordered by directory and their classes by source file, so that the result depends
	 * on the sources and not on the order a filesystem lists them in: a tie between
	 * equally rated classes goes to the first one (#15). A project in which no parser
	 * finds sources has no modules.
	 *
	 * <pre>
	 * List&lt;Module&gt; modules = moduleService.getModules("uploads/bookstore"); // bookstore/model, bookstore/persistence, …
	 * </pre>
	 */
	public List<Module> getModules(String projectDirectory) throws IOException {
		Path project = Paths.get(projectDirectory);
		Map<Path, List<Clazz>> clazzesByDirectory = new LinkedHashMap<>();
		for (ParsedClazz parsed : inSourceOrder(languageParsers.clazzesOf(project), project)) {
			clazzesByDirectory.computeIfAbsent(parsed.sourceFile().getParent(), directory -> new ArrayList<>())
				.add(clazzOf(parsed));
		}
		List<Module> modules = new ArrayList<>();
		clazzesByDirectory.forEach((directory, clazzes) -> modules.add(moduleOf(directory, clazzes)));
		return modules;
	}

	// The sort is stable: classes declared in one file keep the order the parser gave
	// them.
	private static List<ParsedClazz> inSourceOrder(List<ParsedClazz> parsedClazzes, Path project) {
		return parsedClazzes.stream()
			.sorted(Comparator.comparing(ParsedClazz::sourceFile, SourceFileOrder.within(project)))
			.toList();
	}

	private Clazz clazzOf(ParsedClazz parsed) {
		String directoryName = parsed.sourceFile().getParent().getFileName().toString();
		// Similarities are calculated later, when the module's reference classes are
		// elected.
		Clazz clazz = new Clazz(generateNewUUID(), parsed.name(), parsed.dependencies(), 0.0, 0.0, directoryName,
				directoryName, "ALLOW");
		clazz.setPackageName(parsed.packageName());
		// Identifies the class in other classes' references after a move (#43)
		clazz.setQualifiedName(parsed.qualifiedName());
		return clazz;
	}

	// Reference classes, dependencies, similarities and violations are calculated later.
	// The id is derived from the directory, so that analysing a project again gives its
	// modules the ids they had.
	private Module moduleOf(Path directory, List<Clazz> clazzes) {
		String name = directory.getParent().getFileName().toString() + '/' + directory.getFileName().toString();
		String id = generateDeterministicUUID(directory.toString());
		log.info("Added new module: {} with ID: {}", name, id);
		return new Module(id, name, null, null, null, clazzes, 0.0, 0.0, 0);
	}

	public ModuleDTO setRefClazzes(String moduleId, List<String> refClazzIds) {
		// Load all modules
		List<ModuleDTO> modules = getModulesFromFile();

		// Find the target module by moduleId
		Optional<ModuleDTO> targetModuleOptional = modules.stream()
			.filter(module -> moduleId.equals(module.id()))
			.findFirst();

		if (targetModuleOptional.isEmpty()) {
			return null; // Module not found
		}

		ModuleDTO targetModule = targetModuleOptional.get();

		// Find the requested reference classes from the module's classes
		List<ClazzResponseDTO> refClazzes = Arrays.stream(targetModule.clazzes())
			.filter(clazz -> refClazzIds.contains(clazz.id()))
			.sorted((c1, c2) -> Double.compare(c2.similarity(), c1.similarity())) // Sort
																					// descending
																					// by
																					// similarity
			.toList();

		// Validate that all requested class IDs were found
		if (refClazzes.size() != refClazzIds.size()) {
			throw new IllegalArgumentException("Some requested class IDs were not found in the module");
		}

		// Calculate dependencies and similarities
		Module moduleEntity = moduleMapper.toEntity(targetModule);
		List<Clazz> modulesList = refClazzes.stream().map(clazzMapper::toEntity).toList();
		moduleEntity.setRefClazzes(modulesList);
		this.calculateAvgSimilarityWithRefClazzes(moduleEntity);
		this.calculateModuleSimilarity(moduleEntity);
		this.populateRefClazzesDependencies(moduleEntity);
		this.populateModuleDependencies(moduleEntity);
		this.calculateModuleViolations(moduleEntity);

		// Create updated module with new reference classes
		ModuleDTO updatedModule = new ModuleDTO(targetModule.id(), targetModule.name(),
				refClazzes.toArray(new ClazzResponseDTO[0]),
				moduleEntity.getRefClazzesDependencies()
					.stream()
					.map(dependencyMapper::toDto)
					.toArray(DependencyDTO[]::new),
				moduleEntity.getModuleDependencies()
					.stream()
					.map(dependencyMapper::toDto)
					.toArray(DependencyDTO[]::new),
				moduleEntity.getClazzes().stream().map(clazzMapper::toDto).toArray(ClazzResponseDTO[]::new),
				moduleEntity.getSimilarity(), moduleEntity.getAvgRefClazzesSimilarity(), true);

		// Replace the module in the list
		List<ModuleDTO> updatedModules = modules.stream()
			.map(module -> module.id().equals(moduleId) ? updatedModule : module)
			.collect(Collectors.toList());

		// Save updated modules to file
		saveModules(updatedModules);

		return updatedModule;
	}

	public ModuleDTO setRefClazzMode(String moduleId, String classId, String mode) {
		if (!mode.equals("ALLOW") && !mode.equals("MUST")) {
			throw new IllegalArgumentException("Invalid enforceMode: " + mode + "; expected ALLOW or MUST");
		}

		List<ModuleDTO> modules = getModulesFromFile();
		Optional<ModuleDTO> targetOpt = modules.stream().filter(m -> moduleId.equals(m.id())).findFirst();
		if (targetOpt.isEmpty()) {
			return null;
		}

		Module entity = moduleMapper.toEntity(targetOpt.get());

		entity.getRefClazzes()
			.stream()
			.filter(c -> classId.equals(c.getId()))
			.findFirst()
			.ifPresent(c -> c.setEnforceMode(mode));

		entity.getClazzes()
			.stream()
			.filter(c -> classId.equals(c.getId()))
			.findFirst()
			.ifPresent(c -> c.setEnforceMode(mode));

		ModuleDTO updated = moduleMapper.toDto(entity);
		List<ModuleDTO> updatedModules = modules.stream()
			.map(m -> m.id().equals(moduleId) ? updated : m)
			.collect(Collectors.toList());
		saveModules(updatedModules);
		return updated;
	}

}