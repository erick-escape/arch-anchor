package com.archanchor.dto.projects;

import com.archanchor.util.SecretRedaction;

/**
 * Body of {@code POST /api/projects/import}. Only {@code repositoryUrl} is required:
 * {@code ref} defaults to the remote's default branch, {@code subdirectory} to the
 * repository root, {@code projectName} to the repository name, and {@code accessToken} is
 * needed only for private repositories.
 */
public record RepositoryImportRequest(String repositoryUrl, String ref, String subdirectory, String projectName,
		String accessToken) {

	@Override
	public String toString() {
		return "RepositoryImportRequest[repositoryUrl=" + repositoryUrl + ", ref=" + ref + ", subdirectory="
				+ subdirectory + ", projectName=" + projectName + ", accessToken="
				+ SecretRedaction.describe(accessToken) + "]";
	}

}
