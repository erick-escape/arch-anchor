package com.archanchor.services;

import com.archanchor.dto.clazz.ClazzResponseDTO;
import com.archanchor.dto.dependencies.DependencyDTO;
import com.archanchor.dto.module.ModuleDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code examples/bookstore-php} analysed end to end, with the real PHP parser and
 * grammar (#85). It is the bookstore class for class, so {@link BookstoreRunningExample}
 * expects of it the numbers the Java one gets; its README quotes them. The tests written
 * here say why the numbers agree: each class is seen with the dependencies of its Java
 * counterpart, {@code ext-pdo} standing where {@code java.sql} does.
 */
class BookstorePhpRunningExampleTest extends BookstoreRunningExample {

	// What each package of the Java bookstore is called in the PHP one.
	private static final Map<String, String> PHP_PACKAGE_OF = Map.of("java.time", "ext-date", "java.sql", "ext-pdo",
			"bookstore.model", "Bookstore\\Model", "bookstore.persistence", "Bookstore\\Persistence",
			"bookstore.service", "Bookstore\\Service");

	BookstorePhpRunningExampleTest() {
		super("bookstore-php",
				new Layers("Bookstore/Model", "Bookstore/Persistence", "Bookstore/Service", "Bookstore/Web"));
	}

	@Test
	void everyClassDependsOnThePackagesItsJavaCounterpartDoes() throws IOException {
		Map<String, Set<String>> javaPackagesByClass = packagesByClass(javaBookstore());

		Map<String, Set<String>> phpPackagesByClass = packagesByClass(analysed().modulesList());

		assertThat(phpPackagesByClass).hasSize(13);
		javaPackagesByClass.forEach((className, javaPackages) -> assertThat(phpPackagesByClass.get(className))
			.as("the packages %s depends on", className)
			.isEqualTo(javaPackages.stream().map(PHP_PACKAGE_OF::get).collect(Collectors.toSet())));
	}

	// Same sets, so the same numbers: nothing in the metrics depends on the language.
	@Test
	void everyClassIsAsSimilarToItsModuleAsItsJavaCounterpart() throws IOException {
		assertThat(similarityByClass(analysed().modulesList())).isEqualTo(similarityByClass(javaBookstore()));
	}

	@Test
	void classesCarryTheNamesPhpGivesThem() throws IOException {
		ClazzResponseDTO salesReport = clazzNamed("SalesReport", analysed().modulesList());

		assertThat(salesReport.packageName()).isEqualTo("Bookstore\\Web");
		assertThat(salesReport.qualifiedName()).isEqualTo("Bookstore\\Web\\SalesReport");
		assertThat(salesReport.dependencies()).extracting(DependencyDTO::packageName)
			.containsExactly("Bookstore\\Model", "ext-pdo");
		assertThat(salesReport.dependencies().get(1).types()).extracting(DependencyDTO.TypeDTO::fullyQualifiedName)
			.containsExactly("PDO", "PDOStatement");
	}

	@Test
	void theViolationIsTheDatabaseExtensionUsedFromTheWebLayer() throws IOException {
		analysed();

		assertThat(recommendations().violations()).singleElement().satisfies(violation -> {
			assertThat(violation.className()).isEqualTo("SalesReport");
			assertThat(violation.sourceModuleName()).isEqualTo("Bookstore/Web");
			assertThat(violation.violation()).isEqualTo("ext-pdo");
			assertThat(violation.bestSuggestion().targetModuleName()).isEqualTo("Bookstore/Persistence");
		});
	}

	// Analysed apart, as its own project: the two examples share nothing but their shape.
	private static List<ModuleDTO> javaBookstore() throws IOException {
		return new HandWiredAnalysis().projectService.analyzeProject("bookstore", directoryOf("bookstore"))
			.modulesList();
	}

	private static Map<String, Set<String>> packagesByClass(List<ModuleDTO> modules) {
		Map<String, Set<String>> packagesByClass = new LinkedHashMap<>();
		for (ClazzResponseDTO clazz : clazzesOf(modules)) {
			packagesByClass.put(clazz.name(),
					clazz.dependencies().stream().map(DependencyDTO::packageName).collect(Collectors.toSet()));
		}
		return packagesByClass;
	}

	private static Map<String, Double> similarityByClass(List<ModuleDTO> modules) {
		Map<String, Double> similarityByClass = new LinkedHashMap<>();
		clazzesOf(modules).forEach(clazz -> similarityByClass.put(clazz.name(), clazz.similarity()));
		return similarityByClass;
	}

	private static ClazzResponseDTO clazzNamed(String name, List<ModuleDTO> modules) {
		return clazzesOf(modules).stream().filter(clazz -> clazz.name().equals(name)).findFirst().orElseThrow();
	}

	private static List<ClazzResponseDTO> clazzesOf(List<ModuleDTO> modules) {
		return modules.stream().flatMap(module -> List.of(module.clazzes()).stream()).toList();
	}

}
