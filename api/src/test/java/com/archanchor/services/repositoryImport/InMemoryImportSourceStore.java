package com.archanchor.services.repositoryImport;

import com.archanchor.domains.project.ImportSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ImportSourceStore} without the {@code import-source.json} file, so tests leave
 * the projects of whoever runs them alone.
 *
 * <pre>
 * InMemoryImportSourceStore store = new InMemoryImportSourceStore();
 * store.save("shop", new ImportSource("https://github.com/owner/shop", null, null, "abc1234"));
 * </pre>
 */
public class InMemoryImportSourceStore implements ImportSourceStore {

	private final Map<String, ImportSource> sourcesByProject = new HashMap<>();

	@Override
	public Optional<ImportSource> sourceOf(String projectName) {
		return Optional.ofNullable(sourcesByProject.get(projectName));
	}

	@Override
	public void save(String projectName, ImportSource source) {
		sourcesByProject.put(projectName, source);
	}

	@Override
	public void forget(String projectName) {
		sourcesByProject.remove(projectName);
	}

}
