package com.archanchor.services.repositoryImport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.archanchor.dto.projects.ImportedProjectDTO;
import com.archanchor.dto.projects.RepositoryImportRequest;
import com.archanchor.services.ProjectsDirectory;
import com.archanchor.util.DirectoryTrees;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Creates a project from a GitHub repository. The result is indistinguishable from an
 * uploaded project, so listing, analysis and deletion need no changes. Work happens in a
 * staging directory that is always removed, so a failed import leaves nothing behind.
 */
@Slf4j
@Service
public class RepositoryImportService {

	private final ProjectsDirectory projectsDirectory;

	private final RepositoryCloner repositoryCloner;

	private final ClonedTreeCleaner clonedTreeCleaner;

	private final AnalysisRootLocator analysisRootLocator;

	public RepositoryImportService(ProjectsDirectory projectsDirectory, RepositoryCloner repositoryCloner,
			ClonedTreeCleaner clonedTreeCleaner, AnalysisRootLocator analysisRootLocator) {
		this.projectsDirectory = projectsDirectory;
		this.repositoryCloner = repositoryCloner;
		this.clonedTreeCleaner = clonedTreeCleaner;
		this.analysisRootLocator = analysisRootLocator;
	}

	/**
	 * Clones the repository and moves it, or the requested subdirectory of it, into the
	 * projects directory.
	 *
	 * <pre>
	 * importRepository(new RepositoryImportRequest("https://github.com/spring-projects/spring-petclinic",
	 *         null, null, null, null));
	 * // ImportedProjectDTO[name=spring-petclinic, repositoryUrl=..., commitSha=...]
	 * </pre>
	 */
	public ImportedProjectDTO importRepository(RepositoryImportRequest request) throws IOException {
		GitHubRepositoryUrl url = GitHubRepositoryUrl.parse(request.repositoryUrl());
		String projectName = projectNameFor(request, url);
		// Checked before cloning so that a taken name fails fast; adopt() checks again.
		projectsDirectory.requireFreeName(projectName);
		Path staging = projectsDirectory.createStagingDirectory();
		try {
			return importThroughStaging(staging, url, projectName, request);
		}
		finally {
			DirectoryTrees.deleteRecursively(staging);
		}
	}

	private ImportedProjectDTO importThroughStaging(Path staging, GitHubRepositoryUrl url, String projectName,
			RepositoryImportRequest request) throws IOException {
		Path checkout = staging.resolve("checkout");
		String commitSha = repositoryCloner
			.cloneShallow(new CloneRequest(url.cloneUri(), request.ref(), request.accessToken()), checkout);
		clonedTreeCleaner.clean(checkout);
		projectsDirectory.adopt(analysisRootLocator.locate(checkout, request.subdirectory()), projectName);
		log.info("Imported repository {} at commit {} as project {}", url.browseUrl(), commitSha, projectName);
		return new ImportedProjectDTO(projectName, url.browseUrl(), commitSha);
	}

	private static String projectNameFor(RepositoryImportRequest request, GitHubRepositoryUrl url) {
		String requested = request.projectName();
		return requested == null || requested.isBlank() ? url.repository() : requested.strip();
	}

}
