package com.archanchor.services.parsers.php;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Where Composer loads a project's own classes from (#85): the {@code autoload} section
 * of its {@code composer.json}. It is how a PHP project says which of its directories
 * hold production code, whatever framework laid them out: {@code app/} in a Laravel
 * application, {@code src/} in a Symfony one. {@code autoload-dev}, which lists the
 * tests, is left out, as {@code src/test} is for Java (#47).
 *
 * <pre>
 * // { "autoload": { "psr-4": { "App\\": "app/", "Database\\Seeders\\": "database/seeders/" } } }
 * composerAutoload.sourceRootsOf(project); // [project/app, project/database/seeders]
 * </pre>
 */
@Slf4j
@Component
public class ComposerAutoload {

	private static final String MANIFEST = "composer.json";

	// psr-4 and psr-0 give each namespace prefix one path or a list of them; classmap is
	// a list of directories and files. "files" is left out: it lists files of functions.
	private static final List<String> SECTIONS_OF_CLASSES = List.of("psr-4", "psr-0", "classmap");

	private final ObjectMapper json;

	public ComposerAutoload(ObjectMapper json) {
		this.json = json;
	}

	/**
	 * The directories and files the project's classes are loaded from, in the order the
	 * manifest lists them; those that do not exist are left out. Empty when the project
	 * has no {@code composer.json}, or one that autoloads nothing or cannot be read.
	 */
	public List<Path> sourceRootsOf(Path projectDirectory) {
		Path manifest = projectDirectory.resolve(MANIFEST);
		if (!Files.isRegularFile(manifest)) {
			return List.of();
		}
		JsonNode autoload = autoloadOf(manifest);
		Set<Path> roots = new LinkedHashSet<>();
		for (String section : SECTIONS_OF_CLASSES) {
			for (String declaredPath : pathsIn(autoload.path(section))) {
				existingRoot(projectDirectory, declaredPath).ifPresent(roots::add);
			}
		}
		return List.copyOf(roots);
	}

	// A manifest that is not JSON says nothing about where the classes are; the caller
	// then looks everywhere.
	private JsonNode autoloadOf(Path manifest) {
		try {
			return json.readTree(manifest.toFile()).path("autoload");
		}
		catch (IOException unreadable) {
			log.warn("Could not read {} as JSON, so every PHP file of the project is taken for a source: {}", manifest,
					unreadable.getMessage());
			return json.createObjectNode();
		}
	}

	// Every string below the section, whatever holds it: a prefix's value or a list.
	private static List<String> pathsIn(JsonNode section) {
		List<String> paths = new ArrayList<>();
		if (section.isTextual()) {
			paths.add(section.asText());
		}
		section.forEach(child -> paths.addAll(pathsIn(child)));
		return paths;
	}

	// The declared path is normalised by itself, and the project directory left as it
	// was given: "." is then the project directory and not a directory named ".", and a
	// path that climbs out of the project ("../shared") or is absolute is told apart
	// without touching the filesystem.
	private static Optional<Path> existingRoot(Path projectDirectory, String declaredPath) {
		Path insideProject;
		try {
			insideProject = Path.of(declaredPath).normalize();
		}
		catch (InvalidPathException notAPath) {
			log.warn("Expected a path in the autoload section of composer.json, got '{}'", declaredPath);
			return Optional.empty();
		}
		if (insideProject.isAbsolute() || insideProject.startsWith("..")) {
			log.warn("Expected an autoload path inside the project, got '{}': ignored", declaredPath);
			return Optional.empty();
		}
		Path root = insideProject.toString().isEmpty() ? projectDirectory : projectDirectory.resolve(insideProject);
		return Files.exists(root) ? Optional.of(root) : Optional.empty();
	}

}
