package com.archanchor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectsDirectoryTest {

	@TempDir
	Path workspace;

	private Path root;

	private ProjectsDirectory projectsDirectory;

	@BeforeEach
	void setUp() {
		root = workspace.resolve("uploads");
		projectsDirectory = new ProjectsDirectory(root.toString());
	}

	@Test
	void listsProjectDirectoriesInNameOrder() throws IOException {
		Files.createDirectories(root.resolve("shop"));
		Files.createDirectories(root.resolve("bank"));

		assertThat(projectsDirectory.listProjectNames()).containsExactly("bank", "shop");
	}

	@Test
	void listsNoProjectsBeforeTheRootExists() throws IOException {
		assertThat(projectsDirectory.listProjectNames()).isEmpty();
	}

	@Test
	void hidesTheStagingAreaAndPlainFilesFromTheList() throws IOException {
		Files.createDirectories(root.resolve("shop"));
		Files.writeString(root.resolve("notes.txt"), "not a project");
		projectsDirectory.createStagingDirectory();

		assertThat(projectsDirectory.listProjectNames()).containsExactly("shop");
	}

	@Test
	void resolvesUploadedDirectoryNamesThatContainSpaces() {
		assertThat(projectsDirectory.resolveProject("My Project")).isEqualTo(root.resolve("My Project"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", ".", "..", "../outside", "a/b", "a\\b", ".hidden" })
	void refusesNamesThatEscapeTheRootOrHideTheProject(String name) {
		assertThatThrownBy(() -> projectsDirectory.resolveProject(name))
			.isInstanceOf(InvalidProjectNameException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "My Project", "café", "-leading-dash" })
	void restrictsNewProjectNamesToAUrlSafeAlphabet(String name) {
		assertThatThrownBy(() -> projectsDirectory.requireFreeName(name))
			.isInstanceOf(InvalidProjectNameException.class)
			.hasMessageContaining("'" + name + "'");
	}

	@Test
	void acceptsAFreeRepositoryStyleName() {
		projectsDirectory.requireFreeName("spring-petclinic_v2.1");
	}

	@Test
	void refusesANameAlreadyTaken() throws IOException {
		Files.createDirectories(root.resolve("shop"));

		assertThatThrownBy(() -> projectsDirectory.requireFreeName("shop"))
			.isInstanceOf(ProjectAlreadyExistsException.class)
			.hasMessageContaining("'shop'");
	}

	@Test
	void adoptsAStagedTreeAsANewProject() throws IOException {
		Path staged = projectsDirectory.createStagingDirectory().resolve("checkout");
		Files.createDirectories(staged.resolve("src"));

		Path project = projectsDirectory.adopt(staged, "shop");

		assertThat(project).isEqualTo(root.resolve("shop"));
		assertThat(project.resolve("src")).isDirectory();
		assertThat(staged).doesNotExist();
	}

	@Test
	void neverAdoptsOverAnExistingProject() throws IOException {
		Files.createDirectories(root.resolve("shop/src"));
		Path staged = Files.createDirectories(projectsDirectory.createStagingDirectory().resolve("checkout"));

		assertThatThrownBy(() -> projectsDirectory.adopt(staged, "shop"))
			.isInstanceOf(ProjectAlreadyExistsException.class);
		assertThat(root.resolve("shop/src")).isDirectory();
	}

	@Test
	void deletesAProjectAndReportsWhetherItExisted() throws IOException {
		Files.createDirectories(root.resolve("shop/src"));

		assertThat(projectsDirectory.deleteProject("shop")).isTrue();
		assertThat(root.resolve("shop")).doesNotExist();
		assertThat(projectsDirectory.deleteProject("shop")).isFalse();
	}

}
