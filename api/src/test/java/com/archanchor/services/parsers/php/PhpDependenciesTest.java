package com.archanchor.services.parsers.php;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a PHP class depends on (#85): which of the names it writes and imports are
 * dependencies, and the package each is filed under.
 */
class PhpDependenciesTest {

	@Test
	void dependenciesAreGroupedByNamespaceInTheOrderTheyFirstAppear() {
		List<Dependency> dependencies = dependenciesOf("App\\Http", List.of(), "\\App\\Services\\Checkout",
				"\\App\\Models\\Order", "\\App\\Services\\Catalog", "\\App\\Models\\Book");

		assertThat(dependencies).extracting(Dependency::getPackageName).containsExactly("App\\Services", "App\\Models");
		assertThat(typesOf(dependencies.get(0))).containsExactly("App\\Services\\Checkout", "App\\Services\\Catalog");
		assertThat(typesOf(dependencies.get(1))).containsExactly("App\\Models\\Order", "App\\Models\\Book");
		assertThat(dependencies.get(1).getTypes()).extracting(Type::getClassName).containsExactly("Order", "Book");
	}

	@Test
	void aClassNamedSeveralTimesIsOneDependency() {
		List<Dependency> dependencies = dependenciesOf("App\\Http",
				List.of(new ClassImport("App\\Models\\Order", "Order")), "Order", "\\App\\Models\\Order", "order");

		assertThat(dependencies).singleElement()
			.satisfies(models -> assertThat(typesOf(models)).containsExactly("App\\Models\\Order"));
	}

	// Parity with Java, where an import is a dependency whether the class is used or not.
	@Test
	void anImportCountsEvenWhenTheClassIsNotNamedInTheCode() {
		List<Dependency> dependencies = dependenciesOf("App\\Http",
				List.of(new ClassImport("App\\Models\\Order", "Order")));

		assertThat(dependencies).extracting(Dependency::getPackageName).containsExactly("App\\Models");
	}

	@Test
	void anImportOfANamespaceIsNotADependencyButWhatIsNamedThroughItIs() {
		List<Dependency> dependencies = dependenciesOf("App\\Entity",
				List.of(new ClassImport("Doctrine\\ORM\\Mapping", "ORM")), "ORM\\Entity", "ORM\\Column");

		assertThat(dependencies).singleElement().satisfies(mapping -> {
			assertThat(mapping.getPackageName()).isEqualTo("Doctrine\\ORM\\Mapping");
			assertThat(typesOf(mapping)).containsExactly("Doctrine\\ORM\\Mapping\\Entity",
					"Doctrine\\ORM\\Mapping\\Column");
		});
	}

	// As for Java: a module is a directory, and what it uses of itself is not a
	// dependency.
	@Test
	void aClassOfTheSameNamespaceIsNotADependency() {
		List<Dependency> dependencies = dependenciesOf("App\\Models", List.of(), "Customer", "\\app\\models\\Book",
				"\\App\\Services\\Catalog");

		assertThat(dependencies).extracting(Dependency::getPackageName).containsExactly("App\\Services");
	}

	@Test
	void aClassDoesNotDependOnItself() {
		PhpDependencies dependencies = new PhpDependencies(PhpProjectClasses.declaredIn(List.of()),
				new PhpPlatformClasses());
		ClassDeclaration legacyMailer = new ClassDeclaration("LegacyMailer", List.of("LegacyMailer", "legacymailer"));

		assertThat(dependencies.of(new NamespaceBlock("", List.of(), List.of(legacyMailer)), legacyMailer)).isEmpty();
	}

	@Test
	void aClassPhpProvidesIsFiledUnderWhatProvidesIt() {
		List<Dependency> dependencies = dependenciesOf("App\\Reports", List.of(), "\\PDO", "\\RuntimeException",
				"\\PDOStatement", "\\DateTimeImmutable", "\\Countable");

		assertThat(dependencies).extracting(Dependency::getPackageName).containsExactly("ext-pdo", "php", "ext-date");
		assertThat(typesOf(dependencies.get(0))).containsExactly("PDO", "PDOStatement");
		assertThat(typesOf(dependencies.get(1))).containsExactly("RuntimeException", "Countable");
	}

	@Test
	void aBuiltInClassIsSpelledAsPhpSpellsIt() {
		assertThat(typesOf(dependenciesOf("App\\Reports", List.of(), "\\pdo").get(0))).containsExactly("PDO");
	}

	// Written without a backslash or an import, Exception is a class of the namespace it
	// is written in: PHP would look for App\Reports\Exception, and so does the tool.
	@Test
	void aBuiltInNameWrittenWithoutABackslashIsNotTheBuiltInClass() {
		assertThat(dependenciesOf("App\\Reports", List.of(), "Exception")).isEmpty();
		assertThat(dependenciesOf("App\\Reports", List.of(new ClassImport("Exception", "Exception")), "Exception"))
			.extracting(Dependency::getPackageName)
			.containsExactly("php");
	}

	@Test
	void aGlobalClassThatIsNeitherPhpsNorTheProjectsIsFiledUnderGlobal() {
		List<Dependency> dependencies = dependenciesOf("App\\Mail", List.of(), "\\Swift_Mailer", "\\Redis");

		assertThat(dependencies).singleElement().satisfies(global -> {
			assertThat(global.getPackageName()).isEqualTo("(global)");
			assertThat(typesOf(global)).containsExactly("Swift_Mailer", "Redis");
		});
	}

	@Test
	void aClassOfTheProjectIsSpelledAsItsDeclarationSpellsItAndWinsOverABuiltIn() {
		PhpProjectClasses projectClasses = PhpProjectClasses
			.declaredIn(List.of(sourceDeclaring("App\\Models", "Order"), sourceDeclaring("", "Collator")));
		ClassDeclaration report = new ClassDeclaration("Report", List.of("\\app\\models\\ORDER", "\\collator"));

		List<Dependency> dependencies = new PhpDependencies(projectClasses, new PhpPlatformClasses())
			.of(new NamespaceBlock("App\\Reports", List.of(), List.of(report)), report);

		assertThat(dependencies).extracting(Dependency::getPackageName).containsExactly("App\\Models", "(global)");
		assertThat(typesOf(dependencies.get(0))).containsExactly("App\\Models\\Order");
		assertThat(typesOf(dependencies.get(1))).containsExactly("Collator");
	}

	// They share no directory, so they are not one module's classes.
	@Test
	void classesOfTheGlobalNamespaceDependOnEachOther() {
		List<Dependency> dependencies = dependenciesOf("", List.of(), "Swift_Mailer", "\\PDO");

		assertThat(dependencies).extracting(Dependency::getPackageName).containsExactly("(global)", "ext-pdo");
	}

	@Test
	void theDependenciesAreAFreshListEachTime() {
		PhpDependencies dependencies = new PhpDependencies(PhpProjectClasses.declaredIn(List.of()),
				new PhpPlatformClasses());
		ClassDeclaration report = new ClassDeclaration("Report", List.of("\\PDO"));
		NamespaceBlock block = new NamespaceBlock("App\\Reports", List.of(), List.of(report));

		List<Dependency> first = dependencies.of(block, report);
		first.clear();

		assertThat(dependencies.of(block, report)).hasSize(1);
	}

	private static List<Dependency> dependenciesOf(String namespace, List<ClassImport> imports,
			String... writtenClassNames) {
		ClassDeclaration declaration = new ClassDeclaration("Subject", List.of(writtenClassNames));
		NamespaceBlock block = new NamespaceBlock(namespace, imports, List.of(declaration));
		return new PhpDependencies(PhpProjectClasses.declaredIn(List.of()), new PhpPlatformClasses()).of(block,
				declaration);
	}

	private static PhpSourceFacts sourceDeclaring(String namespace, String className) {
		return new PhpSourceFacts(
				List.of(new NamespaceBlock(namespace, List.of(), List.of(new ClassDeclaration(className, List.of())))));
	}

	private static List<String> typesOf(Dependency dependency) {
		return dependency.getTypes().stream().map(Type::getFullyQualifiedName).toList();
	}

}
