package com.archanchor.services.parsers.php;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.services.parsers.LanguageParser;
import com.archanchor.services.parsers.ParsedClazz;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads PHP projects (#85). Its sources are the files Composer autoloads
 * ({@link PhpSourceFiles}); its classes are the classes, interfaces, traits and enums
 * they declare, each under the name PHP gives it ({@code App\Models\Order}, in the
 * package {@code App\Models}); a class depends on the classes its file imports and on
 * those it names where PHP expects a class ({@link PhpDependencies}).
 * <p>
 * A file may declare several classes or none: a template, a file of routes or a migration
 * returning an anonymous class is not a class, as {@code package-info.java} is not one
 * (#31). Nothing here knows a framework: Laravel, Symfony and plain PHP are read by the
 * same rules.
 *
 * <pre>
 * List&lt;ParsedClazz&gt; clazzes = phpParser.clazzesOf(Path.of("uploads/bookstore-php"));
 * </pre>
 */
@Slf4j
@Component
public class PhpParser implements LanguageParser {

	// Far above any class written by hand. A larger file is generated or is data, and
	// reading it would only cost time.
	static final long LARGEST_SOURCE_BYTES = 2L * 1024 * 1024;

	private final PhpSourceFiles sourceFiles;

	private final PhpSourceReader sourceReader;

	private final PhpPlatformClasses platformClasses;

	public PhpParser(PhpSourceFiles sourceFiles, PhpSourceReader sourceReader, PhpPlatformClasses platformClasses) {
		this.sourceFiles = sourceFiles;
		this.sourceReader = sourceReader;
		this.platformClasses = platformClasses;
	}

	@Override
	public String language() {
		return "php";
	}

	@Override
	public List<Path> sourceFilesOf(Path projectDirectory) throws IOException {
		return sourceFiles.in(projectDirectory);
	}

	// Every source is read before any dependency is worked out, because a name is filed
	// by whether the project declares it, which only the whole project can tell.
	@Override
	public List<ParsedClazz> clazzesOf(Path projectDirectory) throws IOException {
		Map<Path, PhpSourceFacts> factsBySource = new LinkedHashMap<>();
		for (Path source : sourceFilesOf(projectDirectory)) {
			factsOf(source).ifPresent(facts -> factsBySource.put(source, facts));
		}
		PhpDependencies dependencies = new PhpDependencies(PhpProjectClasses.declaredIn(factsBySource.values()),
				platformClasses);
		List<ParsedClazz> clazzes = new ArrayList<>();
		factsBySource.forEach((source, facts) -> clazzes.addAll(clazzesDeclaredIn(source, facts, dependencies)));
		log.info("Read {} PHP sources under {}, declaring {} classes", factsBySource.size(), projectDirectory,
				clazzes.size());
		return clazzes;
	}

	// Bytes that are not UTF-8 are replaced rather than refused: they are in a comment or
	// a string of some legacy encoding, and the names around them are still read.
	private Optional<PhpSourceFacts> factsOf(Path source) {
		try {
			if (Files.size(source) > LARGEST_SOURCE_BYTES) {
				log.warn("Left out {}: larger than the {} bytes a source is expected to stay under", source,
						LARGEST_SOURCE_BYTES);
				return Optional.empty();
			}
			return Optional.of(sourceReader.factsOf(new String(Files.readAllBytes(source), StandardCharsets.UTF_8)));
		}
		catch (IOException unreadable) {
			log.warn("Left out {}: it could not be read ({})", source, unreadable.getMessage());
			return Optional.empty();
		}
	}

	private static List<ParsedClazz> clazzesDeclaredIn(Path source, PhpSourceFacts facts,
			PhpDependencies dependencies) {
		List<ParsedClazz> clazzes = new ArrayList<>();
		for (NamespaceBlock block : facts.namespaceBlocks()) {
			for (ClassDeclaration declaration : block.declarations()) {
				clazzes.add(clazzOf(source, block, declaration, dependencies.of(block, declaration)));
			}
		}
		return clazzes;
	}

	// A class of the global namespace declares no package; other classes still refer to
	// it, by its bare name.
	private static ParsedClazz clazzOf(Path source, NamespaceBlock block, ClassDeclaration declaration,
			List<Dependency> dependencies) {
		String packageName = block.namespace().isEmpty() ? null : block.namespace();
		String qualifiedName = PhpClassName.in(block.namespace(), declaration.name());
		return new ParsedClazz(source, declaration.name(), packageName, qualifiedName, dependencies);
	}

}
