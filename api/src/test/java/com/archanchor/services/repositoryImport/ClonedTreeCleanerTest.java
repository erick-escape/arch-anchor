package com.archanchor.services.repositoryImport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ClonedTreeCleanerTest {

	@TempDir
	Path workspace;

	private Path checkout;

	private final ClonedTreeCleaner cleaner = new ClonedTreeCleaner();

	@BeforeEach
	void createCheckout() throws IOException {
		checkout = Files.createDirectories(workspace.resolve("checkout"));
		Files.createDirectories(checkout.resolve(".git/objects"));
		Files.writeString(checkout.resolve(".git/config"), "[core]");
		Files.createDirectories(checkout.resolve("src/main/java/shop"));
		Files.writeString(checkout.resolve("src/main/java/shop/Order.java"), "class Order {}");
		Files.writeString(checkout.resolve("pom.xml"), "<project/>");
	}

	@Test
	void removesTheGitMetadata() throws IOException {
		cleaner.clean(checkout);

		assertThat(checkout.resolve(".git")).doesNotExist();
	}

	@Test
	void removesSymbolicLinksWithoutTouchingTheirTargets() throws IOException {
		Path secret = Files.writeString(workspace.resolve("secret.txt"), "outside the checkout");
		Path fileLink = Files.createSymbolicLink(checkout.resolve("src/main/java/shop/Evil.java"), secret);
		Path directoryLink = Files.createSymbolicLink(checkout.resolve("src/escape"), workspace);

		cleaner.clean(checkout);

		assertThat(Files.exists(fileLink, LinkOption.NOFOLLOW_LINKS)).isFalse();
		assertThat(Files.exists(directoryLink, LinkOption.NOFOLLOW_LINKS)).isFalse();
		assertThat(secret).hasContent("outside the checkout");
	}

	@Test
	void keepsSourcesAndBuildFiles() throws IOException {
		cleaner.clean(checkout);

		assertThat(checkout.resolve("src/main/java/shop/Order.java")).hasContent("class Order {}");
		assertThat(checkout.resolve("pom.xml")).exists();
	}

}
