package com.archanchor.services.repositoryImport;

import com.archanchor.domains.project.ImportSource;
import com.archanchor.util.PerProjectJsonFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Keeps every imported project's source in one JSON file, by default
 * {@code import-source.json} beside {@code rating-weights.json}.
 *
 * <pre>
 * ImportSourceStore store = new JsonFileImportSourceStore("import-source.json");
 * store.save("bookstore", source);
 * </pre>
 */
@Component
public class JsonFileImportSourceStore implements ImportSourceStore {

	private final PerProjectJsonFile<ImportSource> file;

	public JsonFileImportSourceStore(@Value("${archanchor.import-source-file:import-source.json}") String file) {
		this.file = new PerProjectJsonFile<>(Path.of(file), ImportSource.class, "import sources",
				"a JSON object from project name to {repositoryUrl, subdirectory, ref, commitSha}");
	}

	@Override
	public synchronized Optional<ImportSource> sourceOf(String projectName) {
		return file.valueOf(projectName);
	}

	@Override
	public synchronized void save(String projectName, ImportSource source) {
		file.save(projectName, source);
	}

	@Override
	public synchronized void forget(String projectName) {
		file.forget(projectName);
	}

}
