package com.archanchor.services.repositoryImport;

import org.springframework.stereotype.Component;
import com.archanchor.util.DirectoryTrees;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Strips a fresh checkout down to what the analysis reads.
 */
@Component
public class ClonedTreeCleaner {

	/**
	 * Deletes {@code .git/}, which the analysis never reads, and every symbolic link. A
	 * repository can commit a link such as {@code Evil.java -> /etc/passwd}, and the
	 * parser would otherwise follow it out of the checkout.
	 *
	 * <pre>
	 * cleaner.clean(Path.of("uploads/.staging/1f0c.../checkout"));
	 * </pre>
	 */
	public void clean(Path checkout) throws IOException {
		DirectoryTrees.deleteRecursively(checkout.resolve(".git"));
		for (Path link : symbolicLinksUnder(checkout)) {
			Files.delete(link);
		}
	}

	private List<Path> symbolicLinksUnder(Path checkout) throws IOException {
		try (Stream<Path> entries = Files.walk(checkout)) {
			return entries.filter(Files::isSymbolicLink).toList();
		}
	}

}
