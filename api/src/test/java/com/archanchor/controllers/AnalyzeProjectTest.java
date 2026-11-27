package com.archanchor.controllers;

import com.archanchor.services.HandWiredAnalysis;
import com.archanchor.services.ProjectsDirectory;
import com.archanchor.services.repositoryImport.InMemoryImportSourceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/analyze on projects as they lie in the projects directory, read by the real
 * parsers. A project with nothing to analyse is refused with the reason, which the
 * analysis page shows as it is (#87); before, it was analysed as a project without
 * modules, or failed as a bare 500.
 */
class AnalyzeProjectTest {

	@TempDir
	Path projectsRoot;

	@Test
	void analysesAProjectFromItsSources() throws Exception {
		file("shop/src/main/java/shop/orders/Order.java", "package shop.orders; class Order {}");

		analysing("shop").andExpect(status().isOk())
			.andExpect(jsonPath("$.projectName").value("shop"))
			.andExpect(jsonPath("$.modulesList[0].name").value("shop/orders"));
	}

	@Test
	void answers422WhenNoParserFindsSources() throws Exception {
		file("notes/README.md", "# Notes");

		analysing("notes").andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value(startsWith("Found no sources to analyse in 'notes'. Expected ")));
	}

	@Test
	void answers422WhenTheSourcesDeclareNoClass() throws Exception {
		file("site/index.php", "<?php echo 'Hello';");
		file("site/includes/helpers.php", "<?php function greet(): string { return 'Hello'; }");

		analysing("site").andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value("Expected a class in the 2 php source files of 'site', found none: "
					+ "there is nothing to analyse."));
	}

	@Test
	void answers404ForAProjectThatDoesNotExist() throws Exception {
		analysing("missing").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value(startsWith("No project named 'missing'")));
	}

	private ResultActions analysing(String projectName) throws Exception {
		ProjectController projectController = new ProjectController(new HandWiredAnalysis().projectService,
				new ProjectsDirectory(projectsRoot.toString()), new InMemoryImportSourceStore());
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController).build();
		return mockMvc.perform(post("/api/analyze").param("projectName", projectName));
	}

	private void file(String path, String content) throws IOException {
		Path file = projectsRoot.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

}
