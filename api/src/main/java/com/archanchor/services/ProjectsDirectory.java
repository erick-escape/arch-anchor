package com.archanchor.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.archanchor.util.DirectoryTrees;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The directory holding one subdirectory per project, whether it was uploaded from the
 * browser or imported from a repository. It owns the layout: which names are valid, where
 * imports are staged, and how a staged tree becomes a project.
 *
 * <pre>
 * Path staged = projectsDirectory.createStagingDirectory();
 * // ...fill staged...
 * projectsDirectory.adopt(staged, "spring-petclinic"); // uploads/spring-petclinic
 * </pre>
 */
@Component
public class ProjectsDirectory {

	// Dot-directories are never listed as projects, so imports in progress stay
	// invisible.
	private static final String STAGING_DIRECTORY = ".staging";

	// New names end up in URLs (/analyze/:projectName), so they are held to the
	// characters
	// GitHub allows in repository names. Uploaded directories keep whatever name they
	// had.
	private static final Pattern NEW_PROJECT_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");

	private final Path root;

	public ProjectsDirectory(@Value("${archanchor.projects-dir:uploads}") String root) {
		this.root = Path.of(root);
	}

	public Path root() {
		return root;
	}

	public List<String> listProjectNames() throws IOException {
		if (!Files.isDirectory(root)) {
			return List.of();
		}
		try (Stream<Path> children = Files.list(root)) {
			return children.filter(Files::isDirectory)
				.map(child -> child.getFileName().toString())
				.filter(name -> !name.startsWith("."))
				.sorted()
				.toList();
		}
	}

	/**
	 * Resolves an existing project's directory, refusing any name that is not a single
	 * visible directory name so that requests cannot reach outside the root.
	 */
	public Path resolveProject(String name) {
		if (name == null || name.isBlank() || name.startsWith(".") || name.contains("/") || name.contains("\\")) {
			throw new InvalidProjectNameException(
					"Expected a project name that is a single directory name not starting with '.', got '" + name
							+ "'");
		}
		return root.resolve(name);
	}

	public void requireFreeName(String name) {
		if (name == null || !NEW_PROJECT_NAME.matcher(name).matches()) {
			throw new InvalidProjectNameException("Expected a project name of letters, digits, '.', '_' or '-' "
					+ "that starts with a letter or digit, at most 100 characters, got '" + name + "'");
		}
		if (Files.exists(resolveProject(name), LinkOption.NOFOLLOW_LINKS)) {
			throw new ProjectAlreadyExistsException(name);
		}
	}

	public Path createStagingDirectory() throws IOException {
		return Files.createDirectories(root.resolve(STAGING_DIRECTORY).resolve(UUID.randomUUID().toString()));
	}

	/**
	 * Moves a staged tree into place as a new project. Staging lives under the root, so
	 * this is a rename on one filesystem, and it never replaces an existing project.
	 */
	public Path adopt(Path stagedTree, String name) throws IOException {
		requireFreeName(name);
		try {
			return Files.move(stagedTree, resolveProject(name));
		}
		catch (FileAlreadyExistsException e) {
			throw new ProjectAlreadyExistsException(name);
		}
	}

	public boolean deleteProject(String name) throws IOException {
		Path project = resolveProject(name);
		if (!Files.isDirectory(project, LinkOption.NOFOLLOW_LINKS)) {
			return false;
		}
		DirectoryTrees.deleteRecursively(project);
		return true;
	}

}
