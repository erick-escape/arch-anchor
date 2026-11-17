package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.SourceFileOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The production sources of a PHP project (#85): the {@code .php} files under the
 * directories its {@code composer.json} autoloads ({@link ComposerAutoload}) or, when it
 * names none, every {@code .php} file of the project. Either way third-party code is not
 * the project's: the packages Composer installed in {@code vendor/}, those of
 * {@code node_modules/}, and whatever is in a directory whose name starts with a dot.
 *
 * <pre>
 * List&lt;Path&gt; sources = phpSourceFiles.in(Path.of("uploads/shop")); // uploads/shop/app/Models/Order.php, …
 * </pre>
 */
@Slf4j
@Component
public class PhpSourceFiles {

	private static final String SOURCE_EXTENSION = ".php";

	private static final String NODE_PACKAGES = "node_modules";

	private static final String COMPOSER_PACKAGES = "vendor";

	// Composer writes it into the directory it installs packages in; a directory of the
	// project's own that happens to be called vendor has none.
	private static final String COMPOSER_AUTOLOADER = "autoload.php";

	private final ComposerAutoload composerAutoload;

	public PhpSourceFiles(ComposerAutoload composerAutoload) {
		this.composerAutoload = composerAutoload;
	}

	/**
	 * The sources in reading order ({@link SourceFileOrder}); none if there is no such
	 * directory.
	 */
	public List<Path> in(Path projectDirectory) throws IOException {
		if (!Files.isDirectory(projectDirectory)) {
			return List.of();
		}
		List<Path> autoloaded = composerAutoload.sourceRootsOf(projectDirectory);
		SortedSet<Path> sources = new TreeSet<>(SourceFileOrder.within(projectDirectory));
		for (Path root : autoloaded.isEmpty() ? List.of(projectDirectory) : autoloaded) {
			Files.walkFileTree(root, new SourceCollector(root, sources));
		}
		return List.copyOf(sources);
	}

	// Walks one root without entering third-party directories, which is what keeps a
	// project with tens of thousands of installed files quick to list. Symbolic links are
	// not followed, so nothing outside the project is reached.
	private static final class SourceCollector extends SimpleFileVisitor<Path> {

		private final Path root;

		private final SortedSet<Path> sources;

		SourceCollector(Path root, SortedSet<Path> sources) {
			this.root = root;
			this.sources = sources;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
			boolean thirdParty = !directory.equals(root) && isNotTheProjectsCode(directory);
			return thirdParty ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
			String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
			if (attributes.isRegularFile() && fileName.endsWith(SOURCE_EXTENSION)) {
				sources.add(file);
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException unreadable) {
			log.warn("Could not read {}, so it is not among the project's sources: {}", file, unreadable.getMessage());
			return FileVisitResult.CONTINUE;
		}

		private static boolean isNotTheProjectsCode(Path directory) {
			String name = directory.getFileName().toString();
			if (name.startsWith(".") || name.equals(NODE_PACKAGES)) {
				return true;
			}
			return name.equals(COMPOSER_PACKAGES) && Files.isRegularFile(directory.resolve(COMPOSER_AUTOLOADER));
		}

	}

}
