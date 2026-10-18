package com.archanchor.dto.projects;

import com.archanchor.domains.project.ImportSource;

/**
 * Where an imported project came from, as {@code GET /api/projects} sends it (#66).
 */
public record ImportSourceDTO(String repositoryUrl, String subdirectory, String ref, String commitSha) {

	public static ImportSourceDTO of(ImportSource source) {
		return new ImportSourceDTO(source.repositoryUrl(), source.subdirectory(), source.ref(), source.commitSha());
	}

}
