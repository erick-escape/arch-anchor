package com.archanchor.services.parsers.php;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.services.parsers.ParsedClazz;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How what a PHP project's files say becomes the project's classes (#85). The files hold
 * a label each, and a {@link CannedPhpSourceReader} says what a file of that label
 * declares, so nothing here depends on PHP's syntax.
 */
class PhpParserTest {

	@TempDir
	Path workspace;

	private final CannedPhpSourceReader reader = new CannedPhpSourceReader();

	private final PhpParser phpParser = new PhpParser(new PhpSourceFiles(new ComposerAutoload(new ObjectMapper())),
			reader, new PhpPlatformClasses());

	@Test
	void itReadsPhp() {
		assertThat(phpParser.language()).isEqualTo("php");
	}

	@Test
	void aDeclarationBecomesAClassNamedAsPhpNamesIt() throws IOException {
		Path project = projectWithSources("app/Models/Order.php", "order");
		reader.answering("order", declaring("App\\Models", "Order"));

		assertThat(phpParser.clazzesOf(project)).singleElement().satisfies(order -> {
			assertThat(order.sourceFile()).isEqualTo(project.resolve("app/Models/Order.php"));
			assertThat(order.name()).isEqualTo("Order");
			assertThat(order.packageName()).isEqualTo("App\\Models");
			assertThat(order.qualifiedName()).isEqualTo("App\\Models\\Order");
		});
	}

	// Not after its file: PHP does not require the two to agree.
	@Test
	void aClassIsNamedAsItIsDeclaredWhateverItsFileIsCalled() throws IOException {
		Path project = projectWithSources("lib/class.mailer.php", "mailer");
		reader.answering("mailer", declaring("", "LegacyMailer"));

		assertThat(phpParser.clazzesOf(project)).singleElement().satisfies(mailer -> {
			assertThat(mailer.name()).isEqualTo("LegacyMailer");
			assertThat(mailer.packageName()).isNull();
			assertThat(mailer.qualifiedName()).isEqualTo("LegacyMailer");
		});
	}

	@Test
	void aFileMayDeclareSeveralClassesInSeveralNamespaces() throws IOException {
		Path project = projectWithSources("lib/shapes.php", "shapes");
		reader
			.answering("shapes", new PhpSourceFacts(List.of(
					new NamespaceBlock("Shapes", List.of(),
							List.of(new ClassDeclaration("Square", List.of()),
									new ClassDeclaration("Circle", List.of()))),
					new NamespaceBlock("Shapes\\Solid", List.of(), List.of(new ClassDeclaration("Cube", List.of()))))));

		assertThat(phpParser.clazzesOf(project)).extracting(ParsedClazz::qualifiedName)
			.containsExactly("Shapes\\Square", "Shapes\\Circle", "Shapes\\Solid\\Cube");
		assertThat(phpParser.clazzesOf(project)).extracting(ParsedClazz::sourceFile)
			.containsOnly(project.resolve("lib/shapes.php"));
	}

	// A template, a file of routes, a migration returning an anonymous class (#31).
	@Test
	void aFileThatDeclaresNoClassIsNotAClass() throws IOException {
		Path project = projectWithSources("routes/web.php", "routes", "app/Models/Order.php", "order");
		reader.answering("order", declaring("App\\Models", "Order"));

		assertThat(phpParser.sourceFilesOf(project)).hasSize(2);
		assertThat(phpParser.clazzesOf(project)).extracting(ParsedClazz::name).containsExactly("Order");
	}

	@Test
	void classesComeInTheReadingOrderOfTheirFiles() throws IOException {
		Path project = projectWithSources("app/Models/Order.php", "order", "app/Http/Kernel.php", "kernel",
				"app/Models/Book.php", "book");
		reader.answering("order", declaring("App\\Models", "Order"))
			.answering("kernel", declaring("App\\Http", "Kernel"))
			.answering("book", declaring("App\\Models", "Book"));

		assertThat(phpParser.clazzesOf(project)).extracting(ParsedClazz::name)
			.containsExactly("Kernel", "Book", "Order");
	}

	@Test
	void aClassDependsOnWhatItImportsAndNamesFiledByPackage() throws IOException {
		Path project = projectWithSources("app/Reports/SalesReport.php", "report", "app/Models/Book.php", "book");
		reader.answering("book", declaring("App\\Models", "Book"))
			.answering("report", new PhpSourceFacts(List.of(new NamespaceBlock("App\\Reports",
					List.of(new ClassImport("App\\Models\\Book", "Book"), new ClassImport("PDO", "PDO")),
					List.of(new ClassDeclaration("SalesReport", List.of("PDO", "book", "\\Redis", "Summary")))))));

		ParsedClazz salesReport = phpParser.clazzesOf(project)
			.stream()
			.filter(clazz -> clazz.name().equals("SalesReport"))
			.findFirst()
			.orElseThrow();

		assertThat(salesReport.dependencies()).extracting(Dependency::getPackageName)
			.containsExactly("App\\Models", "ext-pdo", "(global)");
		assertThat(salesReport.dependencies().get(0).getTypes().get(0).getFullyQualifiedName())
			.isEqualTo("App\\Models\\Book");
	}

	// Legacy code is often Latin-1: an accent in a comment must not cost the file.
	@Test
	void bytesThatAreNotUtf8AreReplacedNotRefused() throws IOException {
		Path project = projectWithSources("lib/Cafe.php", "placeholder");
		Files.write(project.resolve("lib/Cafe.php"), new byte[] { 'c', 'a', 'f', (byte) 0xE9 });

		phpParser.clazzesOf(project);

		assertThat(reader.sourcesRead()).containsExactly("caf�");
	}

	@Test
	void aFileTooLargeToBeWrittenByHandIsNotRead() throws IOException {
		Path project = projectWithSources("lib/Small.php", "small", "lib/generated.php", "");
		Files.write(project.resolve("lib/generated.php"), new byte[(int) PhpParser.LARGEST_SOURCE_BYTES + 1]);
		reader.answering("small", declaring("Lib", "Small"));

		assertThat(phpParser.clazzesOf(project)).extracting(ParsedClazz::name).containsExactly("Small");
		assertThat(reader.sourcesRead()).containsExactly("small");
	}

	@Test
	void aProjectWithoutPhpHasNoSourcesAndNoClasses() throws IOException {
		Path javaProject = projectWithSources("src/main/java/shop/Catalog.java", "class Catalog {}");

		assertThat(phpParser.sourceFilesOf(javaProject)).isEmpty();
		assertThat(phpParser.clazzesOf(javaProject)).isEmpty();
	}

	private static PhpSourceFacts declaring(String namespace, String className) {
		return new PhpSourceFacts(
				List.of(new NamespaceBlock(namespace, List.of(), List.of(new ClassDeclaration(className, List.of())))));
	}

	// Pairs of a file and its content.
	private Path projectWithSources(String... filesAndContents) throws IOException {
		Path project = workspace.resolve("shop");
		for (int index = 0; index < filesAndContents.length; index += 2) {
			Path file = project.resolve(filesAndContents[index]);
			Files.createDirectories(file.getParent());
			Files.writeString(file, filesAndContents[index + 1], StandardCharsets.UTF_8);
		}
		return project;
	}

}
