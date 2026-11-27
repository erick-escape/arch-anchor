package com.archanchor.services.parsers;

import com.archanchor.domains.dependency.Dependency;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A parser of a language the tool does not read, reporting the classes a test wrote down
 * for it instead of reading sources. It shows what the tool does with any parser's
 * answer, whatever the language.
 *
 * <pre>
 * ScriptedLanguageParser php = new ScriptedLanguageParser("php").declaring(project.resolve("app/Models/User.php"),
 * 		"User", "App\\Models", "App\\Models\\User");
 * </pre>
 */
public final class ScriptedLanguageParser implements LanguageParser {

	private final String language;

	private final List<ParsedClazz> clazzes = new ArrayList<>();

	private final List<Path> sourcesDeclaringNothing = new ArrayList<>();

	public ScriptedLanguageParser(String language) {
		this.language = language;
	}

	/** Adds a class without dependencies, declared in the given file. */
	public ScriptedLanguageParser declaring(Path sourceFile, String name, String packageName, String qualifiedName) {
		clazzes.add(new ParsedClazz(sourceFile, name, packageName, qualifiedName, new ArrayList<Dependency>()));
		return this;
	}

	/** Adds a source in which no class is declared, such as a file of functions. */
	public ScriptedLanguageParser listing(Path sourceFile) {
		sourcesDeclaringNothing.add(sourceFile);
		return this;
	}

	@Override
	public String language() {
		return language;
	}

	@Override
	public String expectedSources() {
		return language + " sources where a test puts them";
	}

	@Override
	public List<Path> sourceFilesOf(Path projectDirectory) {
		List<Path> sources = new ArrayList<>(clazzes.stream().map(ParsedClazz::sourceFile).distinct().toList());
		sources.addAll(sourcesDeclaringNothing);
		return sources;
	}

	@Override
	public List<ParsedClazz> clazzesOf(Path projectDirectory) {
		return List.copyOf(clazzes);
	}

}
