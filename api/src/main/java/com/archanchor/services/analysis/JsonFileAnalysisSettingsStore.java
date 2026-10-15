package com.archanchor.services.analysis;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.util.PerProjectJsonFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Keeps every project's analysis settings in one JSON file, by default
 * {@code analysis-settings.json} beside {@code rating-weights.json}. A file of its own,
 * so the rating weights' file keeps the shape it had (#36).
 *
 * <pre>
 * AnalysisSettingsStore store = new JsonFileAnalysisSettingsStore("analysis-settings.json");
 * store.save("mybatis-3", new AnalysisSettings(2, 2, 1, 5, 0.2));
 * </pre>
 */
@Component
public class JsonFileAnalysisSettingsStore implements AnalysisSettingsStore {

	private final PerProjectJsonFile<AnalysisSettings> file;

	public JsonFileAnalysisSettingsStore(
			@Value("${archanchor.analysis-settings-file:analysis-settings.json}") String file) {
		this.file = new PerProjectJsonFile<>(Path.of(file), AnalysisSettings.class, "analysis settings",
				"a JSON object from project name to {minSplitModuleClasses, minSourceClassesAfterMove, "
						+ "maxMoveTargetsPerClass, minClassDependencies, minRecommendationRate}");
	}

	@Override
	public synchronized AnalysisSettings settingsOf(String projectName) {
		return file.valueOf(projectName).orElse(AnalysisSettings.DEFAULT);
	}

	@Override
	public synchronized void save(String projectName, AnalysisSettings settings) {
		file.save(projectName, settings);
	}

	@Override
	public synchronized void forget(String projectName) {
		file.forget(projectName);
	}

}
