package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The classes a PHP project declares, found however a reference cases their name (#85).
 */
class PhpProjectClassesTest {

	private final PhpProjectClasses declared = PhpProjectClasses
		.declaredIn(List.of(sourceDeclaring("App\\Models", "Order", "Customer"), sourceDeclaring("", "LegacyMailer"),
				sourceDeclaring("app\\models", "ORDER")));

	@Test
	void aDeclaredClassIsFoundByItsQualifiedName() {
		assertThat(declared.declaredNameOf("App\\Models\\Customer")).contains("App\\Models\\Customer");
		assertThat(declared.declaredNameOf("LegacyMailer")).contains("LegacyMailer");
	}

	@Test
	void aNameIsFoundHoweverItIsCasedAndComesBackAsDeclared() {
		assertThat(declared.declaredNameOf("app\\models\\customer")).contains("App\\Models\\Customer");
		assertThat(declared.declaredNameOf("legacymailer")).contains("LegacyMailer");
	}

	@Test
	void ofTwoDeclarationsOfOneNameTheFirstReadKeepsIt() {
		assertThat(declared.declaredNameOf("App\\Models\\Order")).contains("App\\Models\\Order");
	}

	@Test
	void aClassTheProjectDoesNotDeclareIsNotFound() {
		assertThat(declared.declaredNameOf("App\\Models\\Invoice")).isEmpty();
		assertThat(declared.declaredNameOf("Order")).isEmpty();
	}

	private static PhpSourceFacts sourceDeclaring(String namespace, String... classNames) {
		List<ClassDeclaration> declarations = List.of(classNames)
			.stream()
			.map(name -> new ClassDeclaration(name, List.of()))
			.toList();
		return new PhpSourceFacts(List.of(new NamespaceBlock(namespace, List.of(), declarations)));
	}

}
