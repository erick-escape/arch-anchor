package com.archanchor.controllers;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;
import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static com.archanchor.controllers.ShopBackend.PROJECT;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.ARCHIVE;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.PERSISTENCE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The architect tunes which recommendations the analyses make and show (#36), on the shop
 * of {@link AnalysedShopFixture}. With the defaults it gets one split of the web module
 * (its second half is {@code SalesReport} alone), one move of {@code SalesReport}, one
 * merge and one violation entry.
 */
class ProjectAnalysisSettingsTest {

	private static final String OTHER_PROJECT = "bookstore";

	@TempDir
	Path projectsRoot;

	@Test
	void aProjectStartsWithSettingsThatFilterNothing() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		settingsOf(backend, PROJECT).andExpect(status().isOk())
			.andExpect(jsonPath("$.minSplitModuleClasses").value(1))
			.andExpect(jsonPath("$.minSourceClassesAfterMove").value(1))
			.andExpect(jsonPath("$.maxMoveTargetsPerClass").value(nullValue()))
			.andExpect(jsonPath("$.minClassDependencies").value(0))
			.andExpect(jsonPath("$.minRecommendationRate").value(0.0))
			.andExpect(jsonPath("$.minMergeSimilarityChange").value(nullValue()))
			.andExpect(jsonPath("$.minMoveSimilarityChange").value(nullValue()))
			.andExpect(jsonPath("$.minSplitSimilarityChange").value(nullValue()));
		recommendations(backend).andExpect(jsonPath("$.splits", hasSize(1)))
			.andExpect(jsonPath("$.moves", hasSize(1)))
			.andExpect(jsonPath("$.merges", hasSize(1)))
			.andExpect(jsonPath("$.violations", hasSize(1)));
	}

	@Test
	void savedSettingsAreTheProjectsFromThenOn() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(2, 3, 1, 4, 0.25, null)).andExpect(status().isOk())
			.andExpect(jsonPath("$.maxMoveTargetsPerClass").value(1));

		settingsOf(backend, PROJECT).andExpect(jsonPath("$.minSplitModuleClasses").value(2))
			.andExpect(jsonPath("$.minSourceClassesAfterMove").value(3))
			.andExpect(jsonPath("$.minClassDependencies").value(4))
			.andExpect(jsonPath("$.minRecommendationRate").value(0.25));
	}

	@Test
	void aSplitWithAHalfBelowTheMinimumIsNotRecommended() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(2, 1, null, 0, 0.0, null)).andExpect(status().isOk());

		recommendations(backend).andExpect(jsonPath("$.splits", empty())).andExpect(jsonPath("$.moves", hasSize(1)));
	}

	// The web module has four classes, so moving SalesReport out leaves three.
	@Test
	void aMoveThatLeavesItsSourceBelowTheMinimumIsNotRecommended() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(1, 4, null, 0, 0.0, null)).andExpect(status().isOk());
		recommendations(backend).andExpect(jsonPath("$.moves", empty()));

		setSettings(backend, PROJECT, settings(1, 3, null, 0, 0.0, null)).andExpect(status().isOk());
		recommendations(backend).andExpect(jsonPath("$.moves", hasSize(1)));
	}

	@Test
	void onlyTheBestRatedMoveTargetsOfAClassAreRecommended() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndTwoDatabaseModules);
		recommendations(backend).andExpect(jsonPath("$.moves[*].targetModuleName", contains(PERSISTENCE, ARCHIVE)));

		setSettings(backend, PROJECT, settings(1, 1, 1, 0, 0.0, null)).andExpect(status().isOk());

		recommendations(backend).andExpect(jsonPath("$.moves[*].targetModuleName", contains(PERSISTENCE)));
	}

	// SalesReport depends on two packages, java.sql and shop.model. The module still
	// counts its violation, so the merge that removes it is rated as before.
	@Test
	void aClassWithTooFewDependenciesGetsNoMoveAndNoViolationEntry() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(1, 1, null, 3, 0.0, null)).andExpect(status().isOk());

		recommendations(backend).andExpect(jsonPath("$.moves", empty()))
			.andExpect(jsonPath("$.violations", empty()))
			.andExpect(jsonPath("$.merges[0].metrics.module1Violations").value(1));
	}

	// The merge is rated about 0.41, the split and the move above 0.5. Violation entries
	// have no rate and are always shown.
	@Test
	void recommendationsRatedBelowTheMinimumAreHiddenButViolationsAreNot() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(1, 1, null, 0, 0.5, null)).andExpect(status().isOk());

		recommendations(backend).andExpect(jsonPath("$.merges", empty()))
			.andExpect(jsonPath("$.splits", hasSize(1)))
			.andExpect(jsonPath("$.moves", hasSize(1)))
			.andExpect(jsonPath("$.violations", hasSize(1)))
			.andExpect(jsonPath("$.summary.totalRecommendations").value(3));
	}

	// #41: the shop's merge costs 0.175 of similarity and removes the web module's
	// violation.
	@Test
	void aMergeThatCostsMoreSimilarityThanAllowedIsNotRecommended() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(1, 1, null, 0, 0.0, 0.0)).andExpect(status().isOk());
		recommendations(backend).andExpect(jsonPath("$.merges", empty())).andExpect(jsonPath("$.moves", hasSize(1)));

		setSettings(backend, PROJECT, settings(1, 1, null, 0, 0.0, -0.2)).andExpect(status().isOk());
		recommendations(backend).andExpect(jsonPath("$.merges", hasSize(1)));
	}

	// #73: the shop's split separates SalesReport and raises similarity by 0.125; its
	// move
	// raises it too, so a limit of 0 keeps it.
	@Test
	void aSplitThatChangesSimilarityByLessThanTheMinimumIsNotRecommended() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, withSimilarityLimits(settings(1, 1, null, 0, 0.0, null), 0.0, 0.3))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.minMoveSimilarityChange").value(0.0))
			.andExpect(jsonPath("$.minSplitSimilarityChange").value(0.3));
		recommendations(backend).andExpect(jsonPath("$.splits", empty())).andExpect(jsonPath("$.moves", hasSize(1)));

		setSettings(backend, PROJECT, withSimilarityLimits(settings(1, 1, null, 0, 0.0, null), 0.0, 0.25))
			.andExpect(status().isOk());
		recommendations(backend).andExpect(jsonPath("$.splits", hasSize(1)));
	}

	// Settings saved before #73, or sent without the new limits, mean no limit.
	@Test
	void aMissingMoveOrSplitLimitMeansNoLimit() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(2, 1, null, 0, 0.0, 0.0)).andExpect(status().isOk());

		settingsOf(backend, PROJECT).andExpect(jsonPath("$.minMoveSimilarityChange").value(nullValue()))
			.andExpect(jsonPath("$.minSplitSimilarityChange").value(nullValue()));
	}

	@Test
	void aMoveLimitOutsideMinusOneToOneIsRejectedNamingIt() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, withSimilarityLimits(settings(1, 1, null, 0, 0.0, null), 2.0, null))
			.andExpect(status().isBadRequest())
			.andExpect(
					jsonPath("$.detail", allOf(containsString("minMoveSimilarityChange"), containsString("[-1, 1]"))));
	}

	// The settings do not affect the election, so nothing is re-elected.
	@Test
	void newSettingsRunTheAnalysesOnce() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(2, 1, null, 0, 0.0, null)).andExpect(status().isOk());

		assertEquals(1, backend.analysisRunsSinceSetUp());
	}

	@Test
	void settingsOfAProjectOtherThanTheAnalysedOneAreOnlyStored() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, OTHER_PROJECT, settings(2, 1, null, 0, 0.0, null)).andExpect(status().isOk());

		settingsOf(backend, OTHER_PROJECT).andExpect(jsonPath("$.minSplitModuleClasses").value(2));
		recommendations(backend).andExpect(jsonPath("$.splits", hasSize(1)));
		assertEquals(0, backend.analysisRunsSinceSetUp());
	}

	@Test
	void aSettingBelowItsRangeIsRejectedNamingItAndTheRange() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(0, 1, null, 0, 0.0, null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail",
					allOf(containsString("minSplitModuleClasses"), containsString(">= 1"), containsString("got 0"))));
		setSettings(backend, PROJECT, settings(1, 1, null, 0, 1.5, null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", allOf(containsString("minRecommendationRate"), containsString("[0, 1]"))));
	}

	// A missing minimum read as 0 would pass for minClassDependencies.
	@Test
	void aMissingSettingIsRejectedRatherThanReadAsZero() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT,
				"{\"minSplitModuleClasses\":1,\"minSourceClassesAfterMove\":1,"
						+ "\"maxMoveTargetsPerClass\":null,\"minRecommendationRate\":0.0}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", containsString("minClassDependencies")));
	}

	@Test
	void aRejectedChangeKeepsTheSettingsAndRunsNothing() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		setSettings(backend, PROJECT, settings(0, 1, null, 0, 0.0, null)).andExpect(status().isBadRequest());

		settingsOf(backend, PROJECT).andExpect(jsonPath("$.minSplitModuleClasses").value(1));
		assertEquals(0, backend.analysisRunsSinceSetUp());
	}

	@Test
	void anUnknownProjectIsNotFound() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);

		settingsOf(backend, "no-such-project").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail", containsString("no-such-project")));
		setSettings(backend, "no-such-project", settings(1, 1, null, 0, 0.0, null)).andExpect(status().isNotFound());
	}

	// Otherwise a new project of the same name would start from the old one's settings.
	@Test
	void deletingAProjectForgetsItsSettings() throws Exception {
		ShopBackend backend = analysed(AnalysedShopFixture::webAndPersistence);
		setSettings(backend, OTHER_PROJECT, settings(2, 1, null, 0, 0.0, null)).andExpect(status().isOk());

		backend.mockMvc.perform(delete("/api/projects/" + OTHER_PROJECT)).andExpect(status().isOk());

		assertEquals(AnalysisSettings.DEFAULT, backend.analysisSettingsStore.settingsOf(OTHER_PROJECT));
	}

	private ShopBackend analysed(Function<ModuleService, List<Module>> fixture) throws IOException {
		Files.createDirectories(projectsRoot.resolve(PROJECT));
		Files.createDirectories(projectsRoot.resolve(OTHER_PROJECT));
		return ShopBackend.analysed(fixture, projectsRoot);
	}

	private static ResultActions recommendations(ShopBackend backend) throws Exception {
		return backend.mockMvc.perform(get("/api/recommendations"));
	}

	private static ResultActions settingsOf(ShopBackend backend, String projectName) throws Exception {
		return backend.mockMvc.perform(get("/api/projects/" + projectName + "/analysis-settings"));
	}

	private static ResultActions setSettings(ShopBackend backend, String projectName, String body) throws Exception {
		return backend.mockMvc
			.perform(put("/api/projects/" + projectName + "/analysis-settings").contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private static String settings(int minSplitModuleClasses, int minSourceClassesAfterMove,
			Integer maxMoveTargetsPerClass, int minClassDependencies, double minRecommendationRate,
			Double minMergeSimilarityChange) {
		return "{\"minSplitModuleClasses\":" + minSplitModuleClasses + ",\"minSourceClassesAfterMove\":"
				+ minSourceClassesAfterMove + ",\"maxMoveTargetsPerClass\":" + maxMoveTargetsPerClass
				+ ",\"minClassDependencies\":" + minClassDependencies + ",\"minRecommendationRate\":"
				+ minRecommendationRate + ",\"minMergeSimilarityChange\":" + minMergeSimilarityChange + "}";
	}

	private static String withSimilarityLimits(String settings, Double minMoveSimilarityChange,
			Double minSplitSimilarityChange) {
		return settings.substring(0, settings.length() - 1) + ",\"minMoveSimilarityChange\":" + minMoveSimilarityChange
				+ ",\"minSplitSimilarityChange\":" + minSplitSimilarityChange + "}";
	}

}
