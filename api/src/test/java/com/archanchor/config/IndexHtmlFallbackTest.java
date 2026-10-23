package com.archanchor.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #77: the built frontend's files, or its index.html for a client-side route, never for
 * the API.
 */
class IndexHtmlFallbackTest {

	@TempDir
	Path builtFrontend;

	private Resource location;

	private final IndexHtmlFallback resolver = new IndexHtmlFallback();

	@BeforeEach
	void setUp() {
		location = new FileSystemResource(builtFrontend + "/");
	}

	@Test
	void servesAFileTheBuildProduced() throws IOException {
		write("index.html");
		write("assets/app.js");

		assertThat(resolved("assets/app.js")).isEqualTo(builtFrontend.resolve("assets/app.js"));
	}

	// BrowserRouter routes exist only in the browser, so a reload of one must get the
	// app.
	@Test
	void servesTheIndexForAClientSideRoute() throws IOException {
		write("index.html");

		assertThat(resolved("analyze/bookstore")).isEqualTo(builtFrontend.resolve("index.html"));
		assertThat(resolved("projects")).isEqualTo(builtFrontend.resolve("index.html"));
	}

	// A mistyped API call must stay a 404, not come back as a page of HTML.
	@Test
	void neverServesTheIndexForAnApiPath() throws IOException {
		write("index.html");

		assertThat(resolver.getResource("api/no-such-endpoint", location)).isNull();
	}

	// The dev server and the tests run without a built frontend.
	@Test
	void findsNothingWithoutABuiltFrontend() throws IOException {
		assertThat(resolver.getResource("analyze/bookstore", location)).isNull();
	}

	private Path resolved(String requestPath) throws IOException {
		return resolver.getResource(requestPath, location).getFile().toPath();
	}

	private void write(String relativePath) throws IOException {
		Path file = builtFrontend.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, relativePath);
	}

}
