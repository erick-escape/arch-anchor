package com.archanchor.services.repositoryImport;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Chooses the directory of a checkout that becomes the project. ModuleService.getModules
 * only looks for {@code src/} directly under the project root, and many repositories keep
 * theirs one or two levels down, so a root without {@code src/} is refused with the
 * directories that would work.
 */
@Component
public class AnalysisRootLocator {

	private static final String SOURCE_DIRECTORY = "src";

	// Deep enough for layouts like services/orders/backend/src.
	private static final int CANDIDATE_SEARCH_DEPTH = 4;

	private static final int MAX_CANDIDATES = 5;

	/**
	 * Returns the checkout itself, or the subdirectory the user named, once it is known
	 * to hold {@code src/}.
	 *
	 * <pre>
	 * locator.locate(checkout, "examples/sample-architecture-project");
	 * </pre>
	 */
	public Path locate(Path checkout, String subdirectory) throws IOException {
		Path root = resolveInside(checkout, subdirectory);
		if (!Files.isDirectory(root.resolve(SOURCE_DIRECTORY), LinkOption.NOFOLLOW_LINKS)) {
			throw new MissingSourceDirectoryException(describeMissingSource(checkout, subdirectory));
		}
		return root;
	}

	private Path resolveInside(Path checkout, String subdirectory) {
		if (subdirectory == null || subdirectory.isBlank()) {
			return checkout;
		}
		try {
			Path relative = Path.of(subdirectory.strip()).normalize();
			Path resolved = checkout.resolve(relative).normalize();
			if (!relative.isAbsolute() && resolved.startsWith(checkout)) {
				return resolved;
			}
		}
		catch (InvalidPathException e) {
			// Reported below with the same message as any other unusable path.
		}
		throw new InvalidSubdirectoryException("Expected a subdirectory relative to the repository root, such as "
				+ "'backend', got '" + subdirectory + "'");
	}

	private String describeMissingSource(Path checkout, String subdirectory) throws IOException {
		String location = subdirectory == null || subdirectory.isBlank() ? "the repository root"
				: "'" + subdirectory + "'";
		List<String> candidates = directoriesHoldingSource(checkout);
		String hint = candidates.isEmpty()
				? "No directory within " + CANDIDATE_SEARCH_DEPTH + " levels of the root holds src/."
				: "Set the subdirectory to one of: " + String.join(", ", candidates) + ".";
		return "Expected a src/ directory directly under " + location + ", found none. " + hint;
	}

	private List<String> directoriesHoldingSource(Path checkout) throws IOException {
		try (Stream<Path> entries = Files.walk(checkout, CANDIDATE_SEARCH_DEPTH)) {
			return entries
				.filter(entry -> entry.getFileName().toString().equals(SOURCE_DIRECTORY)
						&& Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS))
				.map(source -> checkout.relativize(source.getParent()))
				.sorted(Comparator.comparingInt(Path::getNameCount).thenComparing(Path::toString))
				.limit(MAX_CANDIDATES)
				.map(candidate -> candidate.toString().isEmpty() ? "." : candidate.toString().replace('\\', '/'))
				.toList();
		}
	}

}
