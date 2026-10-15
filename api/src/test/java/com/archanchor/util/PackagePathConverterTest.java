package com.archanchor.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringJUnitConfig
public class PackagePathConverterTest {

	private final PackageNameExtractor packageNameExtractor = new PackageNameExtractor();

	@Test
	void testExtractPackageName() {
		// Test main example
		String input = "com.archanchor.domains.dependency.Dependency";
		String expected = "com.archanchor.domains.dependency";
		String actual = packageNameExtractor.extractPackageName(input);
		assertEquals(expected, actual);
	}

	@Test
	void testExtractPackageNameWithSingleClass() {
		String input = "MyClass";
		String expected = "";
		String actual = packageNameExtractor.extractPackageName(input);
		assertEquals(expected, actual);
	}

	@Test
	void testExtractPackageNameWithEmptyString() {
		String input = "";
		String expected = "";
		String actual = packageNameExtractor.extractPackageName(input);
		assertEquals(expected, actual);
	}

	@Test
	void testExtractPackageNameWithNull() {
		String input = null;
		String expected = "";
		String actual = packageNameExtractor.extractPackageName(input);
		assertEquals(expected, actual);
	}

	@Test
	void testExtractPackageNameWithNestedPackage() {
		String input = "com.example.MyClass";
		String expected = "com.example";
		String actual = packageNameExtractor.extractPackageName(input);
		assertEquals(expected, actual);
	}

	// #35: a nested type's enclosing class is not a package.
	@Test
	void aNestedTypeBelongsToThePackageOfItsEnclosingClass() {
		assertEquals("java.util", packageNameExtractor.extractPackageName("java.util.Map.Entry"));
		assertEquals("org.apache.ibatis.annotations",
				packageNameExtractor.extractPackageName("org.apache.ibatis.annotations.Select.List"));
	}

	@Test
	void aStaticMemberBelongsToThePackageOfItsClass() {
		assertEquals("org.apache.ibatis.annotations", packageNameExtractor
			.extractPackageName("org.apache.ibatis.annotations.AnnotationConstants.NULL_TYPE_DISCRIMINATOR"));
	}

	@Test
	void testAlternativeMethod() {
		String input = "com.archanchor.domains.dependency.Dependency";
		String expected = "com.archanchor.domains.dependency";
		String actual = packageNameExtractor.extractPackageNameAlternative(input);
		assertEquals(expected, actual);
	}

}