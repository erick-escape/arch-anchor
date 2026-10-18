package com.archanchor.controllers;

import com.archanchor.domains.project.ImportSource;
import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/projects says where each imported project came from (#66). */
class ProjectListTest {

	private static final ImportSource BOOKSTORE = new ImportSource("https://github.com/erick-escape/arch-anchor",
			"examples/bookstore", "main", "3ab2e30c0ffee");

	@TempDir
	Path projectsRoot;

	private ShopBackend backend;

	@BeforeEach
	void setUp() throws IOException {
		Files.createDirectories(projectsRoot.resolve("bookstore"));
		Files.createDirectories(projectsRoot.resolve(ShopBackend.PROJECT));
		backend = ShopBackend.analysed(AnalysedShopFixture::webAndPersistence, projectsRoot);
		backend.importSourceStore.save("bookstore", BOOKSTORE);
	}

	@Test
	void listsAnImportedProjectWithItsRepositoryAndAnUploadedOneWithoutSource() throws Exception {
		backend.mockMvc.perform(get("/api/projects"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.projects[0].name").value("bookstore"))
			.andExpect(jsonPath("$.projects[0].source.repositoryUrl").value(BOOKSTORE.repositoryUrl()))
			.andExpect(jsonPath("$.projects[0].source.subdirectory").value("examples/bookstore"))
			.andExpect(jsonPath("$.projects[0].source.ref").value("main"))
			.andExpect(jsonPath("$.projects[0].source.commitSha").value("3ab2e30c0ffee"))
			.andExpect(jsonPath("$.projects[1].name").value(ShopBackend.PROJECT))
			.andExpect(jsonPath("$.projects[1].source").value(nullValue()));
	}

	// Otherwise a folder uploaded later under the same name would claim the repository.
	@Test
	void deletingAProjectForgetsWhereItCameFrom() throws Exception {
		backend.mockMvc.perform(delete("/api/projects/bookstore")).andExpect(status().isOk());

		assertThat(backend.importSourceStore.sourceOf("bookstore")).isEmpty();
	}

}
