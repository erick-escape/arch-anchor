package com.archanchor.services.analysis;

import com.archanchor.domains.analysis.AnalysisSettings;
import org.springframework.stereotype.Component;

/**
 * The analysis settings in force: those of the project whose modules are saved. The
 * server holds one analysed project at a time ({@code modules.bin}), so there is one set
 * of settings, replaced when another project is analysed or when the architect changes
 * them (#36). It mirrors {@code ActiveRatingWeights}.
 *
 * <pre>
 * activeSettings.adopt("mybatis-3", analysisSettingsStore.settingsOf("mybatis-3"));
 * if (activeSettings.current().shows(rate)) { ... }
 * </pre>
 */
@Component
public class ActiveAnalysisSettings {

	// One field, so a reader never sees the settings of one project paired with the name
	// of another.
	private volatile InForce inForce;

	/** The defaults, for no project in particular, until a project is analysed. */
	public ActiveAnalysisSettings() {
		this(AnalysisSettings.DEFAULT);
	}

	public ActiveAnalysisSettings(AnalysisSettings settings) {
		this.inForce = new InForce(null, settings);
	}

	public AnalysisSettings current() {
		return inForce.settings();
	}

	public void adopt(String projectName, AnalysisSettings settings) {
		inForce = new InForce(projectName, settings);
	}

	/** Whether the saved modules, and so these settings, belong to that project. */
	public boolean appliesTo(String projectName) {
		return projectName != null && projectName.equals(inForce.projectName());
	}

	private record InForce(String projectName, AnalysisSettings settings) {
	}

}
