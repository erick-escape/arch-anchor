package com.archanchor.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.MISPLACED_CLASS;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.PERSISTENCE;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.WEB;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.idOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression suite for issue #19: applying a recommendation had no lasting effect. The
 * requests are the ones the recommendations panel sends.
 */
class RecommendationsControllerTest {

	private final ShopBackend backend = ShopBackend.analysed();

	@Test
	void splitRecommendationListsTheIdsOfTheClassesNotTheirNames() throws Exception {
		recommendations().andExpect(jsonPath("$.splits[0].originalModuleName").value(WEB))
			.andExpect(jsonPath("$.splits[0].module2ClassIds", contains(idOf(MISPLACED_CLASS))))
			.andExpect(jsonPath("$.splits[0].module1ClassIds", hasSize(3)));
	}

	@Test
	void applySplitAcceptsTheClassIdsTheRecommendationLists() throws Exception {
		String recommended = recommendations().andReturn().getResponse().getContentAsString();
		String moduleId = JsonPath.read(recommended, "$.splits[0].originalModuleId");
		List<String> classIds = JsonPath.read(recommended, "$.splits[0].module2ClassIds");

		applySplit(moduleId, classIds).andExpect(status().isOk()).andExpect(jsonPath("$.newModules", hasSize(2)));
	}

	@Test
	void moveRecommendationNamesTheSourceAndTargetModules() throws Exception {
		recommendations().andExpect(jsonPath("$.moves[0].className").value(MISPLACED_CLASS))
			.andExpect(jsonPath("$.moves[0].sourceModuleName").value(WEB))
			.andExpect(jsonPath("$.moves[0].targetModuleName").value(PERSISTENCE));
	}

	@Test
	void applyingTheMoveLeavesNothingToRecommend() throws Exception {
		recommendations().andExpect(jsonPath("$.violations", hasSize(1))).andExpect(jsonPath("$.moves", hasSize(1)));

		backend.mockMvc
			.perform(post("/api/recommendations/apply/move").param("classId", idOf(MISPLACED_CLASS))
				.param("sourceModuleId", idOf(WEB))
				.param("targetModuleId", idOf(PERSISTENCE)))
			.andExpect(status().isOk());

		recommendations().andExpect(jsonPath("$.summary.totalRecommendations").value(0));
	}

	@Test
	void applyingTheMergeStopsRecommendingIt() throws Exception {
		recommendations().andExpect(jsonPath("$.merges", hasSize(1)));

		backend.mockMvc
			.perform(post("/api/recommendations/apply/merge").param("sourceId", idOf(WEB))
				.param("targetId", idOf(PERSISTENCE)))
			.andExpect(status().isOk());

		recommendations().andExpect(jsonPath("$.merges", empty()));
	}

	// The module operation an apply delegates to refreshes too; running twice would only
	// cost time (#28).
	@Test
	void applyingACardRunsTheAnalysesOnce() throws Exception {
		backend.mockMvc
			.perform(post("/api/recommendations/apply/move").param("classId", idOf(MISPLACED_CLASS))
				.param("sourceModuleId", idOf(WEB))
				.param("targetModuleId", idOf(PERSISTENCE)))
			.andExpect(status().isOk());

		assertEquals(1, backend.analysisRunsSinceSetUp());
	}

	@Test
	void aRejectedApplyLeavesTheRecommendationsAsTheyWere() throws Exception {
		applySplit(idOf(WEB), List.of("not-a-class-id")).andExpect(status().isBadRequest());

		recommendations().andExpect(jsonPath("$.splits", hasSize(1))).andExpect(jsonPath("$.moves", hasSize(1)));
	}

	private ResultActions recommendations() throws Exception {
		return backend.mockMvc.perform(get("/api/recommendations"));
	}

	private ResultActions applySplit(String moduleId, List<String> classIds) throws Exception {
		String quotedIds = String.join(",", classIds.stream().map(id -> '"' + id + '"').toList());
		String body = "{\"moduleId\":\"" + moduleId + "\",\"classIds\":[" + quotedIds + "]}";
		return backend.mockMvc
			.perform(post("/api/recommendations/apply/split").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
