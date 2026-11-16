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

	public ScriptedLanguageParser(String language) {
		this.language = language;
	}

	/** Adds a class without dependencies, declared in the given file. */
	public ScriptedLanguageParser declaring(Path sourceFile, String name, String packageName, String qualifiedName) {
		clazzes.add(new ParsedClazz(sourceFile, name, packageName, qualifiedName, new ArrayList<Dependency>()));
		return this;
	}

	@Override
	public String language() {
		return language;
	}

	@Override
	public List<Path> sourceFilesOf(Path projectDirectory) {
		return clazzes.stream().map(ParsedClazz::sourceFile).distinct().toList();
	}

	@Override
	public List<ParsedClazz> clazzesOf(Path projectDirectory) {
		return List.copyOf(clazzes);
	}

}
