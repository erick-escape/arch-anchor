package com.archanchor.services.repositoryImport;

import com.archanchor.domains.project.ImportSource;

import java.util.Optional;

/**
 * Where each imported project came from, kept after the import returns (#66).
 *
 * <pre>
 * importSourceStore.save("bookstore", source);
 * Optional&lt;ImportSource&gt; source = importSourceStore.sourceOf("bookstore");
 * </pre>
 */
public interface ImportSourceStore {

	/** The project's source, or empty for a project uploaded as a folder. */
	Optional<ImportSource> sourceOf(String projectName);

	void save(String projectName, ImportSource source);

	/** So a project uploaded later under the same name does not claim the repository. */
	void forget(String projectName);

}
