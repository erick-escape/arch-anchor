package com.archanchor.dto.projects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryImportRequestTest {

	@Test
	void redactsTheAccessTokenWhenPrinted() {
		RepositoryImportRequest request = new RepositoryImportRequest("https://github.com/owner/repo", null, null, null,
				"github_pat_secret");

		assertThat(request.toString()).doesNotContain("github_pat_secret")
			.contains("<redacted>")
			.contains("https://github.com/owner/repo");
	}

	@Test
	void saysWhenThereIsNoToken() {
		RepositoryImportRequest request = new RepositoryImportRequest("https://github.com/owner/repo", null, null, null,
				null);

		assertThat(request.toString()).contains("accessToken=<none>");
	}

}
