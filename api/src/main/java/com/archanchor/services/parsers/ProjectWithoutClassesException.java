package com.archanchor.services.parsers;

import java.nio.file.Path;
import java.util.List;

/**
 * A project the tool can read nothing from (#87): no parser finds sources in it, or the
 * sources found declare no class. Analysed anyway it would be a project without modules,
 * which says nothing about why; the message says what was looked for, and is written to
 * be shown to the user as it is.
 */
public class ProjectWithoutClassesException extends RuntimeException {

	private ProjectWithoutClassesException(String message) {
		super(message);
	}

	/**
	 * No parser finds sources in the project.
	 *
	 * <pre>
	 * throw ProjectWithoutClassesException.withoutSources(Path.of("uploads/notes"), List.of("Java sources under src/main/"));
	 * </pre>
	 */
	public static ProjectWithoutClassesException withoutSources(Path projectDirectory, List<String> expectedSources) {
		return new ProjectWithoutClassesException("Found no sources to analyse in '" + nameOf(projectDirectory)
				+ "'. Expected " + String.join("; or ", expectedSources) + ".");
	}

	/**
	 * The sources the project's parser found declare no class: scripts, templates,
	 * configuration.
	 */
	public static ProjectWithoutClassesException withoutClasses(Path projectDirectory, String language,
			int sourceFiles) {
		String files = sourceFiles == 1 ? "source file" : "source files";
		return new ProjectWithoutClassesException("Expected a class in the " + sourceFiles + " " + language + " "
				+ files + " of '" + nameOf(projectDirectory) + "', found none: there is nothing to analyse.");
	}

	private static Path nameOf(Path projectDirectory) {
		Path name = projectDirectory.toAbsolutePath().normalize().getFileName();
		return name == null ? projectDirectory : name;
	}

}
