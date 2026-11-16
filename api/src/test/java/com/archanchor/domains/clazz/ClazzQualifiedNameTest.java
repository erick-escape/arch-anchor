package com.archanchor.domains.clazz;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The name other classes refer to a class by (#83): the one its parser gave it, as its
 * language writes it.
 */
class ClazzQualifiedNameTest {

	@Test
	void aClassIsKnownByTheNameItsParserGaveIt() {
		Clazz user = clazzNamed("User");
		user.setPackageName("App\\Models");
		user.setQualifiedName("App\\Models\\User");

		assertThat(user.getQualifiedName()).isEqualTo("App\\Models\\User");
	}

	// Only Java was read before classes stored their name, and Java joins with a dot.
	@Test
	void aClassSavedWithoutItsNameIsTheJavaClassOfItsPackage() {
		Clazz controller = clazzNamed("CatalogController");
		controller.setPackageName("shop.web");

		assertThat(controller.getQualifiedName()).isEqualTo("shop.web.CatalogController");
	}

	@Test
	void aClassWithNeitherNameNorPackageHasNoQualifiedName() {
		assertThat(clazzNamed("Unpackaged").getQualifiedName()).isNull();
	}

	private static Clazz clazzNamed(String name) {
		return new Clazz("c1", name, List.of(), 0.0, 0.0, "web", "web", "ALLOW");
	}

}
