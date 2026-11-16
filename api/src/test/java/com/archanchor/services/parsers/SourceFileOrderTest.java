package com.archanchor.services.parsers;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order sources are read and listed in (#15, #81): by directory, then by file name,
 * as plain strings.
 */
class SourceFileOrderTest {

	private static final Path PROJECT = Path.of("uploads", "shop");

	@Test
	void aDirectoryComesBeforeTheDirectoriesInsideIt() {
		assertThat(ordered("src/shop/web/Zebra.java", "src/shop/web/admin/Audit.java", "src/shop/Shop.java"))
			.containsExactly("src/shop/Shop.java", "src/shop/web/Zebra.java", "src/shop/web/admin/Audit.java");
	}

	@Test
	void filesOfOneDirectoryAreOrderedByNameWithCapitalsFirst() {
		assertThat(ordered("src/shop/UrlMapper.java", "src/shop/URLParser.java", "src/shop/Uri.java"))
			.containsExactly("src/shop/URLParser.java", "src/shop/Uri.java", "src/shop/UrlMapper.java");
	}

	private static List<String> ordered(String... sources) {
		return Stream.of(sources)
			.map(PROJECT::resolve)
			.sorted(SourceFileOrder.within(PROJECT))
			.map(file -> PROJECT.relativize(file).toString().replace('\\', '/'))
			.toList();
	}

}
