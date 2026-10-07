package com.archanchor.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.archanchor.services.ProjectsDirectory;
import com.archanchor.services.repositoryImport.AnalysisRootLocator;
import com.archanchor.services.repositoryImport.ClonedTreeCleaner;
import com.archanchor.services.repositoryImport.FakeRepositoryCloner;
import com.archanchor.services.repositoryImport.RepositoryCloneFailedException;
import com.archanchor.services.repositoryImport.RepositoryImportService;
import com.archanchor.services.repositoryImport.RepositoryNotAccessibleException;
import com.archanchor.services.repositoryImport.RepositoryRefNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepositoryImportControllerTest {

	@TempDir
	Path workspace;

	private Path root;

	@BeforeEach
	void setUp() {
		root = workspace.resolve("uploads");
	}

	@Test
	void createsTheProjectAndReportsTheCommit() throws Exception {
		importing("{\"repositoryUrl\":\"https://github.com/owner/shop\",\"accessToken\":\"github_pat_secret\"}",
				new FakeRepositoryCloner())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("shop"))
			.andExpect(jsonPath("$.repositoryUrl").value("https://github.com/owner/shop"))
			.andExpect(jsonPath("$.commitSha").value(FakeRepositoryCloner.COMMIT_SHA))
			.andExpect(content().string(not(containsString("github_pat_secret"))));
	}

	@Test
	void answers400ForAUrlOutsideGitHub() throws Exception {
		importing("{\"repositoryUrl\":\"https://gitlab.com/owner/shop\"}", new FakeRepositoryCloner())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("https://github.com/owner/repo")));
	}

	@Test
	void answers400ForASubdirectoryOutsideTheRepository() throws Exception {
		importing("{\"repositoryUrl\":\"https://github.com/owner/shop\",\"subdirectory\":\"../outside\"}",
				new FakeRepositoryCloner())
			.andExpect(status().isBadRequest());
	}

	@Test
	void answers404WhenTheRepositoryCannotBeRead() throws Exception {
		importing("{\"repositoryUrl\":\"https://github.com/owner/shop\"}",
				new FakeRepositoryCloner()
					.failingWith(new RepositoryNotAccessibleException("Repository was not found", null)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Repository was not found"));
	}

	@Test
	void answers409WhenTheNameIsTaken() throws Exception {
		Files.createDirectories(root.resolve("shop"));

		importing("{\"repositoryUrl\":\"https://github.com/owner/shop\"}", new FakeRepositoryCloner())
			.andExpect(status().isConflict());
	}

	@Test
	void answers422WhenTheRefDoesNotExist() throws Exception {
		importing("{\"repositoryUrl\":\"https://github.com/owner/shop\",\"ref\":\"nope\"}",
				new FakeRepositoryCloner().failingWith(new RepositoryRefNotFoundException("got 'nope'")))
			.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void answers422WhenTheRootHasNoSources() throws Exception {
		importing("{\"repositoryUrl\":\"https://github.com/owner/shop\",\"subdirectory\":\"examples\"}",
				new FakeRepositoryCloner())
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value(containsString("src/")));
	}

	@Test
	void answers502WhenTheCloneFails() throws Exception {
		importing("{\"repositoryUrl\":\"https://github.com/owner/shop\"}",
				new FakeRepositoryCloner().failingWith(new RepositoryCloneFailedException("connection reset", null)))
			.andExpect(status().isBadGateway());
	}

	private ResultActions importing(String body, FakeRepositoryCloner cloner) throws Exception {
		RepositoryImportService service = new RepositoryImportService(new ProjectsDirectory(root.toString()), cloner,
				new ClonedTreeCleaner(), new AnalysisRootLocator());
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RepositoryImportController(service)).build();
		return mockMvc.perform(post("/api/projects/import").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
