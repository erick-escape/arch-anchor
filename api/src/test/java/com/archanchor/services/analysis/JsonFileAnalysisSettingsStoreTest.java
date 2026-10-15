package com.archanchor.services.analysis;

import com.archanchor.domains.analysis.AnalysisSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFileAnalysisSettingsStoreTest {

	private static final AnalysisSettings STRICT = new AnalysisSettings(2, 2, 1, 5, 0.2, null);

	@TempDir
	Path workspace;

	@Test
	void aProjectWithoutSettingsHasTheDefaults() {
		assertThat(storeIn("analysis-settings.json").settingsOf("mybatis-3")).isEqualTo(AnalysisSettings.DEFAULT);
	}

	@Test
	void savedSettingsOutliveTheStoreThatSavedThem() {
		storeIn("analysis-settings.json").save("mybatis-3", STRICT);

		assertThat(storeIn("analysis-settings.json").settingsOf("mybatis-3")).isEqualTo(STRICT);
	}

	// No limit is saved as null and must come back as no limit, not as 0.
	@Test
	void noLimitOnMoveTargetsSurvivesTheFile() {
		storeIn("analysis-settings.json").save("mybatis-3", AnalysisSettings.DEFAULT);

		assertThat(storeIn("analysis-settings.json").settingsOf("mybatis-3").limitsMoveTargets()).isFalse();
	}

	@Test
	void eachProjectKeepsItsOwnSettings() {
		JsonFileAnalysisSettingsStore store = storeIn("analysis-settings.json");

		store.save("mybatis-3", STRICT);
		store.save("bookstore", AnalysisSettings.DEFAULT);

		assertThat(store.settingsOf("mybatis-3")).isEqualTo(STRICT);
		assertThat(store.settingsOf("bookstore")).isEqualTo(AnalysisSettings.DEFAULT);
	}

	@Test
	void aForgottenProjectIsBackToTheDefaults() {
		JsonFileAnalysisSettingsStore store = storeIn("analysis-settings.json");
		store.save("mybatis-3", STRICT);

		store.forget("mybatis-3");

		assertThat(storeIn("analysis-settings.json").settingsOf("mybatis-3")).isEqualTo(AnalysisSettings.DEFAULT);
	}

	@Test
	void anUnreadableFileIsReportedWithItsPath() throws IOException {
		Files.writeString(workspace.resolve("analysis-settings.json"), "{ not json");

		assertThatThrownBy(() -> storeIn("analysis-settings.json").settingsOf("mybatis-3"))
			.hasMessageContaining(workspace.resolve("analysis-settings.json").toString())
			.hasMessageContaining("project name");
	}

	private JsonFileAnalysisSettingsStore storeIn(String fileName) {
		return new JsonFileAnalysisSettingsStore(workspace.resolve(fileName).toString());
	}

}
