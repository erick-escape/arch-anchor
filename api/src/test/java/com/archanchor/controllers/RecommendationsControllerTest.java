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

	// #72: both were written as 0.0, although the analysis compares them to consider the
	// move at all. SalesReport shares one of two dependencies with the web's reference
	// class and both with the persistence's.
	@Test
	void moveRecommendationCarriesTheClassSimilarityWithBothModulesReferenceClasses() throws Exception {
		recommendations().andExpect(jsonPath("$.moves[0].metrics.classCurrentSimilarity").value(0.5))
			.andExpect(jsonPath("$.moves[0].metrics.classTargetSimilarity").value(1.0));
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

	// #26: the card showed the reference classes' agreement under these labels, so its
	// similarities did not add up to the improvement below them.
	@Test
	void mergeCardShowsTheModuleSimilaritiesItsImprovementIsComputedFrom() throws Exception {
		String recommended = recommendations().andReturn().getResponse().getContentAsString();
		String module1Id = JsonPath.read(recommended, "$.merges[0].module1Id");
		String module2Id = JsonPath.read(recommended, "$.merges[0].module2Id");
		String savedModules = backend.mockMvc.perform(get("/api/module"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String merged = backend.mockMvc
			.perform(post("/api/recommendations/apply/merge").param("sourceId", module1Id).param("targetId", module2Id))
			.andReturn()
			.getResponse()
			.getContentAsString();

		double module1Similarity = similarityOf(savedModules, module1Id);
		double module2Similarity = similarityOf(savedModules, module2Id);
		double mergedSimilarity = JsonPath.<Number>read(merged, "$.similarity").doubleValue();
		assertEquals(module1Similarity, metric(recommended, "module1Similarity"), 1e-9);
		assertEquals(module2Similarity, metric(recommended, "module2Similarity"), 1e-9);
		assertEquals(mergedSimilarity, metric(recommended, "mergedSimilarity"), 1e-9);
		assertEquals(mergedSimilarity - (module1Similarity + module2Similarity) / 2,
				metric(recommended, "similarityImprovement"), 1e-9);
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

	private static double similarityOf(String modules, String moduleId) {
		List<Number> similarities = JsonPath.read(modules, "$[?(@.id == '" + moduleId + "')].similarity");
		return similarities.get(0).doubleValue();
	}

	private static double metric(String recommendations, String name) {
		return JsonPath.<Number>read(recommendations, "$.merges[0].metrics." + name).doubleValue();
	}

	private ResultActions applySplit(String moduleId, List<String> classIds) throws Exception {
		String quotedIds = String.join(",", classIds.stream().map(id -> '"' + id + '"').toList());
		String body = "{\"moduleId\":\"" + moduleId + "\",\"classIds\":[" + quotedIds + "]}";
		return backend.mockMvc
			.perform(post("/api/recommendations/apply/split").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
