package com.archanchor.services.repositoryImport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubRepositoryUrlTest {

	@ParameterizedTest
	@ValueSource(strings = { "https://github.com/spring-projects/spring-petclinic",
			"https://github.com/spring-projects/spring-petclinic/",
			"https://github.com/spring-projects/spring-petclinic.git",
			"https://www.github.com/spring-projects/spring-petclinic", "github.com/spring-projects/spring-petclinic",
			"git@github.com:spring-projects/spring-petclinic.git", "git@github.com:spring-projects/spring-petclinic",
			"  https://github.com/spring-projects/spring-petclinic  ",
			"https://github.com/spring-projects/spring-petclinic?tab=readme-ov-file#readme" })
	void acceptsTheFormsUsersCopyFromGitHub(String rawUrl) {
		GitHubRepositoryUrl url = GitHubRepositoryUrl.parse(rawUrl);

		assertThat(url.owner()).isEqualTo("spring-projects");
		assertThat(url.repository()).isEqualTo("spring-petclinic");
	}

	@Test
	void normalizesEveryFormToAnHttpsCloneUri() {
		GitHubRepositoryUrl url = GitHubRepositoryUrl.parse("git@github.com:spring-projects/spring-petclinic.git");

		assertThat(url.cloneUri()).isEqualTo(URI.create("https://github.com/spring-projects/spring-petclinic.git"));
		assertThat(url.browseUrl()).isEqualTo("https://github.com/spring-projects/spring-petclinic");
	}

	@Test
	void keepsDotsInsideRepositoryNames() {
		GitHubRepositoryUrl url = GitHubRepositoryUrl.parse("https://github.com/octo/octo.github.io.git");

		assertThat(url.repository()).isEqualTo("octo.github.io");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "http://github.com/owner/repo", "https://gitlab.com/owner/repo",
			"https://bitbucket.org/owner/repo", "https://github.com/owner", "file:///etc/passwd",
			"https://github.com.evil.example/owner/repo", "https://evil.example/github.com/owner/repo",
			"https://user:secret@github.com/owner/repo", "https://github.com:8443/owner/repo",
			"https://github.com/../repo", "https://github.com/owner/..", "https://github.com/owner/.",
			"ssh://git@github.com/owner/repo.git" })
	void rejectsAnythingThatIsNotAGitHubRepository(String rawUrl) {
		assertThatThrownBy(() -> GitHubRepositoryUrl.parse(rawUrl)).isInstanceOf(InvalidRepositoryUrlException.class)
			.hasMessageContaining("https://github.com/owner/repo");
	}

	@Test
	void namesTheOffendingValueInTheError() {
		assertThatThrownBy(() -> GitHubRepositoryUrl.parse("https://gitlab.com/owner/repo"))
			.hasMessageContaining("'https://gitlab.com/owner/repo'");
	}

	@Test
	void pointsTreeUrlsToTheBranchAndSubdirectoryFields() {
		assertThatThrownBy(() -> GitHubRepositoryUrl.parse("https://github.com/owner/repo/tree/main/backend"))
			.isInstanceOf(InvalidRepositoryUrlException.class)
			.hasMessageContaining("branch")
			.hasMessageContaining("subdirectory");
	}

}
