package com.archanchor.services.parsers;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * The order a project's sources are read and listed in: by directory, then by file name.
 * It must depend on the sources alone and not on the order a filesystem lists them in,
 * because a tie between equally rated classes goes to the first one (#15). Both are
 * compared as plain strings with {@code /} between directories: Path's own ordering
 * ignores case on some platforms, and the separator would otherwise sort a directory
 * differently next to its siblings from one platform to the next.
 *
 * <pre>
 * sources.sort(SourceFileOrder.within(projectDirectory));
 * </pre>
 */
public final class SourceFileOrder {

	private SourceFileOrder() {
	}

	/** Compares files that lie inside the project directory. */
	public static Comparator<Path> within(Path projectDirectory) {
		Comparator<Path> byDirectory = Comparator
			.comparing(file -> portablePath(projectDirectory.relativize(file.getParent())));
		return byDirectory.thenComparing(file -> file.getFileName().toString());
	}

	private static String portablePath(Path relativePath) {
		return relativePath.toString().replace(File.separatorChar, '/');
	}

}
