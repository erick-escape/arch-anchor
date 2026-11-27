package com.archanchor.services.parsers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * What the tool asks about a project written in one language (#81): which files are its
 * production sources, and which classes they declare, each with what it depends on. Where
 * the sources are, what counts as a class and what package it belongs to differ from one
 * language to the next, so they are answered here; modules, similarity, violations and
 * the analyses are the same for every language and never look at a source file.
 *
 * <pre>
 * List&lt;ParsedClazz&gt; clazzes = javaParser.clazzesOf(Path.of("uploads/bookstore"));
 * </pre>
 */
public interface LanguageParser {

	/**
	 * The language's name in lower case, such as {@code "java"}; no two parsers share it.
	 */
	String language();

	/**
	 * Where the language's sources are looked for, as a phrase written for the user, such
	 * as {@code "Java sources under src/main/, or src/ when there is no src/main/"}. The
	 * tool says it when a project has none (#87).
	 */
	String expectedSources();

	/**
	 * The project's production sources in this language, each inside the project
	 * directory: no tests, no third-party code, no file that can declare no class. Empty
	 * for a project that is not written in the language. Nothing is parsed, so that
	 * asking is cheap.
	 */
	List<Path> sourceFilesOf(Path projectDirectory) throws IOException;

	/**
	 * Every class those sources declare. A file that cannot be parsed must not fail the
	 * project: it yields what can still be told of it, which for Java is its class
	 * without dependencies, and for PHP whatever its readable part declares.
	 */
	List<ParsedClazz> clazzesOf(Path projectDirectory) throws IOException;

}
