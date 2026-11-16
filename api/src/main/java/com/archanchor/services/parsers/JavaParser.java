package com.archanchor.services.parsers;

import com.archanchor.util.PackageDeclaration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * How a Java project is laid out (#81): its sources are under {@code src/}, each
 * {@code .java} file is one class named after the file, and the class belongs to the
 * package the file declares, which with its name is what other classes refer to it by
 * (#83). What a file depends on is left to a {@link JavaDependencyReader}.
 *
 * <pre>
 * List&lt;ParsedClazz&gt; clazzes = new JavaParser(javaParserService).clazzesOf(Path.of("uploads/bookstore"));
 * </pre>
 */
@Component
public class JavaParser implements LanguageParser {

	private static final String SOURCE_DIRECTORY = "src";

	private static final String PRODUCTION_DIRECTORY = "main";

	private static final String SOURCE_EXTENSION = ".java";

	// They declare a package and a Java module, not a class; as classes without
	// dependencies they lowered every module's similarity and anchored a split (#31).
	private static final Set<String> DESCRIPTOR_FILE_NAMES = Set.of("package-info.java", "module-info.java");

	private final JavaDependencyReader dependencyReader;

	public JavaParser(JavaDependencyReader dependencyReader) {
		this.dependencyReader = dependencyReader;
	}

	@Override
	public String language() {
		return "java";
	}

	@Override
	public List<Path> sourceFilesOf(Path projectDirectory) throws IOException {
		Path sources = sourceRoot(projectDirectory.resolve(SOURCE_DIRECTORY));
		if (!Files.isDirectory(sources)) {
			return List.of();
		}
		try (Stream<Path> entries = Files.walk(sources)) {
			return entries.filter(Files::isRegularFile)
				.filter(JavaParser::declaresAClass)
				.sorted(SourceFileOrder.within(projectDirectory))
				.toList();
		}
	}

	@Override
	public List<ParsedClazz> clazzesOf(Path projectDirectory) throws IOException {
		dependencyReader.clearCache();
		return sourceFilesOf(projectDirectory).stream().map(this::clazzDeclaredIn).toList();
	}

	private ParsedClazz clazzDeclaredIn(Path javaSource) {
		String className = withoutExtension(javaSource.getFileName().toString());
		String packageName = PackageDeclaration.of(javaSource);
		return new ParsedClazz(javaSource, className, packageName, qualifiedName(packageName, className),
				dependencyReader.getDependencies(javaSource));
	}

	// A class of the default package cannot be imported, so no other class refers to it.
	private static String qualifiedName(String packageName, String className) {
		return packageName == null ? null : packageName + "." + className;
	}

	/**
	 * Where the sources are read from: {@code src/main} in the standard Maven and Gradle
	 * layout, so that the tests in {@code src/test} are not taken for modules (#47);
	 * {@code src} itself otherwise.
	 *
	 * <pre>
	 * sourceRoot(Path.of("mybatis-3/src")); // mybatis-3/src/main
	 * </pre>
	 */
	private static Path sourceRoot(Path src) {
		Path production = src.resolve(PRODUCTION_DIRECTORY);
		return Files.isDirectory(production) ? production : src;
	}

	private static boolean declaresAClass(Path file) {
		String fileName = file.getFileName().toString();
		return !DESCRIPTOR_FILE_NAMES.contains(fileName) && fileName.toLowerCase().endsWith(SOURCE_EXTENSION);
	}

	private static String withoutExtension(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
	}

}
