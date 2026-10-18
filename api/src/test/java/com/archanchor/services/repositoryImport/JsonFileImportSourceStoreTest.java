package com.archanchor.services.repositoryImport;

import com.archanchor.domains.project.ImportSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFileImportSourceStoreTest {

	private static final ImportSource BOOKSTORE = new ImportSource("https://github.com/erick-escape/arch-anchor",
			"examples/bookstore", null, "3ab2e30c0ffee");

	@TempDir
	Path workspace;

	@Test
	void anUploadedProjectHasNoSource() {
		assertThat(storeIn("import-source.json").sourceOf("bookstore")).isEmpty();
	}

	@Test
	void aSavedSourceOutlivesTheStoreThatSavedIt() {
		storeIn("import-source.json").save("bookstore", BOOKSTORE);

		assertThat(storeIn("import-source.json").sourceOf("bookstore")).contains(BOOKSTORE);
	}

	@Test
	void aForgottenProjectHasNoSource() {
		JsonFileImportSourceStore store = storeIn("import-source.json");
		store.save("bookstore", BOOKSTORE);

		store.forget("bookstore");

		assertThat(storeIn("import-source.json").sourceOf("bookstore")).isEmpty();
	}

	@Test
	void anUnreadableFileIsReportedWithItsPathAndExpectedShape() throws IOException {
		Files.writeString(workspace.resolve("import-source.json"), "{ not json");

		assertThatThrownBy(() -> storeIn("import-source.json").sourceOf("bookstore"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("import-source.json")
			.hasMessageContaining("repositoryUrl");
	}

	private JsonFileImportSourceStore storeIn(String fileName) {
		return new JsonFileImportSourceStore(workspace.resolve(fileName).toString());
	}

}
