package com.archanchor.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Operations on whole directory trees.
 */
public final class DirectoryTrees {

	private DirectoryTrees() {
	}

	/**
	 * Deletes a file, a symbolic link or a directory tree, deepest entries first. Links
	 * are removed, never followed, so a link planted in an imported repository cannot get
	 * anything outside the tree deleted.
	 *
	 * <pre>
	 * DirectoryTrees.deleteRecursively(Path.of("uploads/.staging/1f0c..."));
	 * </pre>
	 */
	public static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		try (Stream<Path> entries = Files.walk(path)) {
			for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(entry);
			}
		}
	}

}
