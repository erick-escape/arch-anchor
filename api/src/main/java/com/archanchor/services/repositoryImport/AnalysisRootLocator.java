package com.archanchor.services.repositoryImport;

import com.archanchor.services.parsers.LanguageParsers;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Chooses the directory of a checkout that becomes the project: one in which a parser
 * finds sources, whatever the layout its language uses (#87). Many repositories keep
 * their project one or two levels down, so a root without sources is refused with what
 * each language expects and the directories that would work.
 */
@Component
public class AnalysisRootLocator {

	// Deep enough for layouts like services/orders/backend/src.
	private static final int CANDIDATE_SEARCH_DEPTH = 4;

	private static final int MAX_CANDIDATES = 5;

	private final LanguageParsers languageParsers;

	public AnalysisRootLocator(LanguageParsers languageParsers) {
		this.languageParsers = languageParsers;
	}

	/**
	 * Returns the checkout itself, or the subdirectory the user named, once a parser is
	 * known to find sources in it.
	 *
	 * <pre>
	 * locator.locate(checkout, "examples/bookstore");
	 * </pre>
	 */
	public Path locate(Path checkout, String subdirectory) throws IOException {
		Path root = resolveInside(checkout, subdirectory);
		if (!holdsSources(root)) {
			throw new MissingSourceDirectoryException(describeMissingSources(checkout, subdirectory));
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

	private boolean holdsSources(Path directory) throws IOException {
		return Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
				&& languageParsers.parserOf(directory).isPresent();
	}

	private String describeMissingSources(Path checkout, String subdirectory) throws IOException {
		String location = subdirectory == null || subdirectory.isBlank() ? "the repository root"
				: "'" + subdirectory + "'";
		List<String> candidates = directoriesHoldingSources(checkout);
		String hint = candidates.isEmpty()
				? "No directory within " + CANDIDATE_SEARCH_DEPTH + " levels of the root holds any."
				: "Set the subdirectory to one of: " + String.join(", ", candidates) + ".";
		return "Found no sources to analyse in " + location + ". Expected "
				+ String.join("; or ", languageParsers.expectedSourcesOfEachLanguage()) + ". " + hint;
	}

	// Level by level, so the shallowest come first; a directory that holds sources is not
	// entered, since every directory below it is part of the same project.
	private List<String> directoriesHoldingSources(Path checkout) throws IOException {
		List<String> candidates = new ArrayList<>();
		List<Path> level = subdirectoriesOf(List.of(checkout));
		for (int depth = 1; depth <= CANDIDATE_SEARCH_DEPTH && candidates.size() < MAX_CANDIDATES; depth++) {
			List<Path> withoutSources = new ArrayList<>();
			for (Path directory : level) {
				if (candidates.size() < MAX_CANDIDATES && holdsSources(directory)) {
					candidates.add(checkout.relativize(directory).toString().replace('\\', '/'));
				}
				else {
					withoutSources.add(directory);
				}
			}
			level = subdirectoriesOf(withoutSources);
		}
		return candidates;
	}

	// Sorted by name, so the candidates come in the same order on every machine. A
	// directory whose name starts with a dot holds tooling, not a project.
	private static List<Path> subdirectoriesOf(List<Path> directories) throws IOException {
		List<Path> subdirectories = new ArrayList<>();
		for (Path directory : directories) {
			try (Stream<Path> entries = Files.list(directory)) {
				entries.filter(entry -> Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS))
					.filter(entry -> !entry.getFileName().toString().startsWith("."))
					.sorted()
					.forEach(subdirectories::add);
			}
		}
		return subdirectories;
	}

}
