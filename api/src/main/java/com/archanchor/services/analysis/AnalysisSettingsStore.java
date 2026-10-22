package com.archanchor.services.analysis;

import com.archanchor.domains.analysis.AnalysisSettings;

/**
 * Where each project's analysis settings are kept between analyses and restarts (#36).
 *
 * <pre>
 * analysisSettingsStore.save("mybatis-3", new AnalysisSettings(2, 2, 1, 5, 0.2, 0.0, 0.0, null));
 * AnalysisSettings settings = analysisSettingsStore.settingsOf("mybatis-3");
 * </pre>
 */
public interface AnalysisSettingsStore {

	/** The project's settings, or {@link AnalysisSettings#DEFAULT} if none were set. */
	AnalysisSettings settingsOf(String projectName);

	void save(String projectName, AnalysisSettings settings);

	/** Back to the defaults, so a new project of the same name starts from them. */
	void forget(String projectName);

}
