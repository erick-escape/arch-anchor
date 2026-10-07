package com.archanchor.services.repositoryImport;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class CloneRequestTest {

	private static final URI REPOSITORY = URI.create("https://github.com/owner/repo.git");

	@Test
	void redactsTheAccessTokenWhenPrinted() {
		CloneRequest request = new CloneRequest(REPOSITORY, "main", "github_pat_secret");

		assertThat(request.toString()).doesNotContain("github_pat_secret").contains("<redacted>");
	}

	@Test
	void treatsBlankFieldsAsAbsent() {
		CloneRequest request = new CloneRequest(REPOSITORY, " ", "");

		assertThat(request.hasRef()).isFalse();
		assertThat(request.hasAccessToken()).isFalse();
	}

}
