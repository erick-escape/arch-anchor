package com.archanchor.services.repositoryImport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.archanchor.dto.projects.ImportedProjectDTO;
import com.archanchor.dto.projects.RepositoryImportRequest;
import com.archanchor.services.ProjectAlreadyExistsException;
import com.archanchor.services.ProjectsDirectory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryImportServiceTest {

	private static final String PETCLINIC = "https://github.com/spring-projects/spring-petclinic";

	@TempDir
	Path workspace;

	private Path root;

	private ProjectsDirectory projectsDirectory;

	private FakeRepositoryCloner cloner;

	@BeforeEach
	void setUp() {
		root = workspace.resolve("uploads");
		projectsDirectory = new ProjectsDirectory(root.toString());
		cloner = new FakeRepositoryCloner();
	}

	@Test
	void importsTheRepositoryUnderItsName() throws IOException {
		ImportedProjectDTO imported = service().importRepository(request(PETCLINIC));

		assertThat(imported)
			.isEqualTo(new ImportedProjectDTO("spring-petclinic", PETCLINIC, FakeRepositoryCloner.COMMIT_SHA));
		assertThat(root.resolve("spring-petclinic/src/main/java/shop/Order.java")).exists();
		assertThat(projectsDirectory.listProjectNames()).containsExactly("spring-petclinic");
	}

	@Test
	void clonesOverHttpsWhateverFormTheUrlCameIn() throws IOException {
		service().importRepository(request("git@github.com:spring-projects/spring-petclinic.git"));

		assertThat(cloner.requests).singleElement()
			.extracting(CloneRequest::uri)
			.isEqualTo(URI.create(PETCLINIC + ".git"));
	}

	@Test
	void passesTheRefAndTokenToTheCloner() throws IOException {
		service().importRepository(new RepositoryImportRequest(PETCLINIC, " v2.1 ", null, null, "github_pat_secret"));

		assertThat(cloner.requests).singleElement()
			.isEqualTo(new CloneRequest(URI.create(PETCLINIC + ".git"), "v2.1", "github_pat_secret"));
	}

	@Test
	void leavesNeitherGitMetadataNorLinksInTheProject() throws IOException {
		service().importRepository(request(PETCLINIC));

		Path project = root.resolve("spring-petclinic");
		assertThat(project.resolve(".git")).doesNotExist();
		assertThat(Files.exists(project.resolve("src/main/java/shop/Linked.java"), LinkOption.NOFOLLOW_LINKS))
			.isFalse();
	}

	@Test
	void keepsOnlyTheNamedSubdirectory() throws IOException {
		service().importRepository(new RepositoryImportRequest(PETCLINIC, null, "examples/demo", "demo", null));

		assertThat(root.resolve("demo/src/main/java/demo/Demo.java")).exists();
		assertThat(root.resolve("demo/pom.xml")).doesNotExist();
	}

	@Test
	void usesTheProjectNameTheUserChose() throws IOException {
		ImportedProjectDTO imported = service()
			.importRepository(new RepositoryImportRequest(PETCLINIC, null, null, " petclinic-v2 ", null));

		assertThat(imported.name()).isEqualTo("petclinic-v2");
		assertThat(root.resolve("petclinic-v2/src")).isDirectory();
	}

	@Test
	void refusesATakenNameBeforeCloning() throws IOException {
		Files.createDirectories(root.resolve("spring-petclinic"));

		assertThatThrownBy(() -> service().importRepository(request(PETCLINIC)))
			.isInstanceOf(ProjectAlreadyExistsException.class);
		assertThat(cloner.requests).isEmpty();
	}

	@Test
	void refusesAnInvalidUrlBeforeCloning() {
		assertThatThrownBy(() -> service().importRepository(request("https://gitlab.com/owner/repo")))
			.isInstanceOf(InvalidRepositoryUrlException.class);
		assertThat(cloner.requests).isEmpty();
	}

	@Test
	void cleansUpWhenTheCloneFails() throws IOException {
		cloner.failingWith(new RepositoryCloneFailedException("connection reset", null));

		assertThatThrownBy(() -> service().importRepository(request(PETCLINIC)))
			.isInstanceOf(RepositoryCloneFailedException.class);
		assertNothingLeftBehind();
	}

	@Test
	void cleansUpWhenTheChosenDirectoryHasNoSources() throws IOException {
		RepositoryImportRequest request = new RepositoryImportRequest(PETCLINIC, null, "examples", null, null);

		assertThatThrownBy(() -> service().importRepository(request))
			.isInstanceOf(MissingSourceDirectoryException.class)
			.hasMessageContaining("examples/demo");
		assertNothingLeftBehind();
	}

	@Test
	void removesTheStagingDirectoryAfterASuccessfulImport() throws IOException {
		service().importRepository(request(PETCLINIC));

		assertThat(root.resolve(".staging")).isEmptyDirectory();
	}

	private RepositoryImportService service() {
		return new RepositoryImportService(projectsDirectory, cloner, new ClonedTreeCleaner(),
				new AnalysisRootLocator());
	}

	private static RepositoryImportRequest request(String repositoryUrl) {
		return new RepositoryImportRequest(repositoryUrl, null, null, null, null);
	}

	private void assertNothingLeftBehind() throws IOException {
		assertThat(projectsDirectory.listProjectNames()).isEmpty();
		assertThat(root.resolve(".staging")).isEmptyDirectory();
	}

}
