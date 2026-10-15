package com.archanchor.services.analysis;

import com.archanchor.domains.analysis.AnalysisSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link AnalysisSettingsStore} without the {@code analysis-settings.json} file, so tests
 * leave the settings of whoever runs them alone.
 *
 * <pre>
 * InMemoryAnalysisSettingsStore store = new InMemoryAnalysisSettingsStore();
 * store.save("shop", new AnalysisSettings(2, 1, null, 0, 0.0));
 * </pre>
 */
public class InMemoryAnalysisSettingsStore implements AnalysisSettingsStore {

	private final Map<String, AnalysisSettings> settingsByProject = new HashMap<>();

	@Override
	public AnalysisSettings settingsOf(String projectName) {
		return settingsByProject.getOrDefault(projectName, AnalysisSettings.DEFAULT);
	}

	@Override
	public void save(String projectName, AnalysisSettings settings) {
		settingsByProject.put(projectName, settings);
	}

	@Override
	public void forget(String projectName) {
		settingsByProject.remove(projectName);
	}

}
