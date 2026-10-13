package com.archanchor.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DirectoryTreesTest {

	@TempDir
	Path workspace;

	@Test
	void deletesANestedTree() throws IOException {
		Path tree = workspace.resolve("tree");
		Files.createDirectories(tree.resolve("src/main/java"));
		Files.writeString(tree.resolve("src/main/java/A.java"), "class A {}");

		DirectoryTrees.deleteRecursively(tree);

		assertThat(tree).doesNotExist();
	}

	@Test
	void deletesSymbolicLinksWithoutTouchingTheirTargets() throws IOException {
		Path outside = Files.writeString(workspace.resolve("outside.txt"), "keep me");
		Path tree = Files.createDirectories(workspace.resolve("tree"));
		Files.createSymbolicLink(tree.resolve("link.txt"), outside);

		DirectoryTrees.deleteRecursively(tree);

		assertThat(tree).doesNotExist();
		assertThat(outside).hasContent("keep me");
	}

	@Test
	void deletesADanglingSymbolicLinkPassedDirectly() throws IOException {
		Path link = Files.createSymbolicLink(workspace.resolve("dangling"), workspace.resolve("missing"));

		DirectoryTrees.deleteRecursively(link);

		assertThat(Files.exists(link, LinkOption.NOFOLLOW_LINKS)).isFalse();
	}

	@Test
	void ignoresAPathThatDoesNotExist() throws IOException {
		DirectoryTrees.deleteRecursively(workspace.resolve("missing"));

		assertThat(workspace).isEmptyDirectory();
	}

}
