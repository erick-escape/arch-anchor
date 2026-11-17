package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpPlatformClasses.PlatformClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The package a class PHP provides is filed under (#85), read from the table generated
 * from the PHP stubs.
 */
class PhpPlatformClassesTest {

	private final PhpPlatformClasses platformClasses = new PhpPlatformClasses();

	@Test
	void theCoreSplAndTheStandardExtensionAreOnePackage() {
		assertThat(platformClasses.find("Exception")).contains(new PlatformClass("Exception", "php"));
		assertThat(platformClasses.find("ArrayObject")).contains(new PlatformClass("ArrayObject", "php"));
		assertThat(platformClasses.find("Generator")).contains(new PlatformClass("Generator", "php"));
	}

	@Test
	void theOtherExtensionsPhpShipsWithAreAPackageEach() {
		assertThat(platformClasses.find("PDO")).contains(new PlatformClass("PDO", "ext-pdo"));
		assertThat(platformClasses.find("PDOStatement")).contains(new PlatformClass("PDOStatement", "ext-pdo"));
		assertThat(platformClasses.find("DateTimeImmutable"))
			.contains(new PlatformClass("DateTimeImmutable", "ext-date"));
		assertThat(platformClasses.find("JsonSerializable"))
			.contains(new PlatformClass("JsonSerializable", "ext-json"));
	}

	// The package is the extension's, not the class's namespace.
	@Test
	void aBuiltInClassInANamespaceIsFiledUnderItsExtension() {
		assertThat(platformClasses.find("Random\\Randomizer"))
			.contains(new PlatformClass("Random\\Randomizer", "ext-random"));
	}

	@Test
	void aNameIsFoundHoweverItsLettersAreCasedAndComesBackAsPhpSpellsIt() {
		assertThat(platformClasses.find("datetimeimmutable"))
			.contains(new PlatformClass("DateTimeImmutable", "ext-date"));
		assertThat(platformClasses.find("pdo")).contains(new PlatformClass("PDO", "ext-pdo"));
	}

	// Laravel and others define global aliases with such names; taking them for the
	// extension's classes would invent a dependency on an extension the project may not
	// even have.
	@Test
	void theClassesOfExtensionsThatAreInstalledSeparatelyAreNotBuiltIn() {
		assertThat(platformClasses.find("Redis")).isEmpty();
		assertThat(platformClasses.find("MongoDB\\Driver\\Manager")).isEmpty();
	}

	@Test
	void aClassOfAProjectIsNotBuiltIn() {
		assertThat(platformClasses.find("App\\Models\\Order")).isEmpty();
		assertThat(platformClasses.find("App\\Exception")).isEmpty();
	}

}
