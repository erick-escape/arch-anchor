package com.archanchor.services;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression suite for issue #35. The parser took everything before a type's last dot as
 * its package, so a nested type became a dependency on its enclosing class: mybatis-3's
 * {@code Select} depended on a package {@code org.apache.ibatis.annotations.Select}, and
 * {@code Map.Entry} on a package {@code java.util.Map}. The sources are parsed for real,
 * in a project without third-party dependencies.
 */
class NestedTypesAreNotPackagesTest {

	@TempDir
	Path project;

	private final JavaParserService javaParserService = new JavaParserService();

	@Test
	void aNestedTypeOfTheSamePackageIsNotADependency() throws IOException {
		source("shop/orders/Order.java", """
				package shop.orders;

				public class Order {
				    public enum Status { OPEN, PAID }
				}
				""");
		Path orderService = source("shop/orders/OrderService.java", """
				package shop.orders;

				public class OrderService {
				    private Order.Status status;
				}
				""");

		assertThat(packagesOf(orderService)).noneMatch(packageName -> packageName.startsWith("shop.orders"));
	}

	@Test
	void anImportedNestedTypeBelongsToThePackageOfItsEnclosingClass() throws IOException {
		Path settings = source("shop/config/Settings.java", """
				package shop.config;

				import java.util.Map.Entry;
				import java.util.Properties;

				public class Settings {
				    private Properties properties;
				    private Entry<String, String> first;
				}
				""");

		List<Dependency> dependencies = javaParserService.getDependencies(settings);

		assertThat(dependencies).extracting(Dependency::getPackageName).containsOnlyOnce("java.util");
		assertThat(dependencies).extracting(Dependency::getPackageName).doesNotContain("java.util.Map");
		assertThat(typesIn(dependencies, "java.util")).contains("java.util.Map.Entry", "java.util.Properties");
	}

	// mybatis-3's repeatable annotations declare their container inside themselves.
	@Test
	void aRepeatableAnnotationDoesNotDependOnItsOwnContainer() throws IOException {
		Path select = source("shop/annotations/Select.java", """
				package shop.annotations;

				import java.lang.annotation.Repeatable;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;

				@Retention(RetentionPolicy.RUNTIME)
				@Repeatable(Select.List.class)
				public @interface Select {
				    String value();

				    @Retention(RetentionPolicy.RUNTIME)
				    @interface List {
				        Select[] value();
				    }
				}
				""");

		assertThat(packagesOf(select)).contains("java.lang.annotation")
			.noneMatch(packageName -> packageName.startsWith("shop.annotations"));
	}

	@Test
	void aStaticImportFromTheSamePackageIsNotADependency() throws IOException {
		source("shop/annotations/AnnotationConstants.java", """
				package shop.annotations;

				public final class AnnotationConstants {
				    public static final String NONE = "";

				    private AnnotationConstants() {
				    }
				}
				""");
		Path defaults = source("shop/annotations/Defaults.java", """
				package shop.annotations;

				import static shop.annotations.AnnotationConstants.NONE;

				public class Defaults {
				    private String column = NONE;
				}
				""");

		assertThat(packagesOf(defaults)).noneMatch(packageName -> packageName.startsWith("shop.annotations"));
	}

	@Test
	void aStaticImportBelongsToThePackageOfTheClassDeclaringTheMember() throws IOException {
		Path timeouts = source("shop/config/Timeouts.java", """
				package shop.config;

				import static java.util.concurrent.TimeUnit.SECONDS;

				public class Timeouts {
				    private long readTimeout = SECONDS.toMillis(5);
				}
				""");

		assertThat(packagesOf(timeouts)).contains("java.util.concurrent")
			.doesNotContain("java.util.concurrent.TimeUnit");
	}

	// The visitor remembered only the last class it entered, so after a nested Date it
	// took java.util.Date for a reference to the class itself.
	@Test
	void aTypeNamedLikeANestedClassIsStillADependency() throws IOException {
		Path report = source("shop/reports/Report.java", """
				package shop.reports;

				public class Report {
				    static class Date {
				    }

				    private java.util.Date printedOn;
				}
				""");

		assertThat(packagesOf(report)).contains("java.util");
	}

	private Path source(String relativePath, String content) throws IOException {
		Files.writeString(project.resolve("pom.xml"), """
				<project><modelVersion>4.0.0</modelVersion>
				<groupId>shop</groupId><artifactId>shop</artifactId><version>1</version></project>
				""");
		Path file = project.resolve("src/main/java").resolve(relativePath);
		Files.createDirectories(file.getParent());
		return Files.writeString(file, content);
	}

	private List<String> packagesOf(Path source) {
		return javaParserService.getDependencies(source).stream().map(Dependency::getPackageName).toList();
	}

	private static List<String> typesIn(List<Dependency> dependencies, String packageName) {
		return dependencies.stream()
			.filter(dependency -> dependency.getPackageName().equals(packageName))
			.flatMap(dependency -> dependency.getTypes().stream())
			.map(Type::getFullyQualifiedName)
			.toList();
	}

}
