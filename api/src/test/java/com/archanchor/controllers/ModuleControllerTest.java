package com.archanchor.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.MISPLACED_CLASS;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.PERSISTENCE;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.WEB;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.idOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModuleControllerTest {

	private final ShopBackend backend = ShopBackend.analysed();

	// The page reloads this after applying a recommendation; re-analysing would discard
	// the change, because module operations are never written back to the sources (#19).
	@Test
	void listsTheSavedModules() throws Exception {
		backend.mockMvc.perform(get("/api/module"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name", contains(WEB, PERSISTENCE)))
			.andExpect(jsonPath("$[0].refClazzes[0].name").value("CatalogController"));
	}

	// A change made by hand left the recommendations of the previous structure on
	// screen, ready to be applied to modules that no longer exist (#28).
	@Test
	void aManualMergeStopsRecommendingIt() throws Exception {
		recommendations().andExpect(jsonPath("$.merges[0].module1Name").exists());

		backend.mockMvc
			.perform(post("/api/module/merge").param("sourceId", idOf(WEB)).param("targetId", idOf(PERSISTENCE)))
			.andExpect(status().isOk());

		recommendations().andExpect(jsonPath("$.merges", empty()))
			.andExpect(jsonPath("$.splits[*].originalModuleId", not(hasItem(idOf(WEB)))));
	}

	@Test
	void aManualSplitDropsTheRecommendationsForTheModuleItReplaced() throws Exception {
		splitWeb(idOf(MISPLACED_CLASS)).andExpect(status().isOk());

		recommendations().andExpect(jsonPath("$.splits[*].originalModuleId", not(hasItem(idOf(WEB)))))
			.andExpect(jsonPath("$.moves[*].sourceModuleId", not(hasItem(idOf(WEB)))))
			.andExpect(jsonPath("$.violations[*].sourceModuleId", not(hasItem(idOf(WEB)))));
	}

	@Test
	void aManualMoveLeavesNothingToRecommend() throws Exception {
		backend.mockMvc
			.perform(post("/api/module/move-class").param("classId", idOf(MISPLACED_CLASS))
				.param("sourceModuleId", idOf(WEB))
				.param("targetModuleId", idOf(PERSISTENCE)))
			.andExpect(status().isOk());

		recommendations().andExpect(jsonPath("$.summary.totalRecommendations").value(0));
	}

	@Test
	void renamingAModuleRenamesItInTheRecommendations() throws Exception {
		backend.mockMvc.perform(post("/api/module/rename").param("moduleId", idOf(WEB)).param("newName", "shop/ui"))
			.andExpect(status().isOk());

		recommendations().andExpect(jsonPath("$.moves[0].sourceModuleName").value("shop/ui"));
	}

	@Test
	void deletingAModuleDropsTheRecommendationsThatInvolveIt() throws Exception {
		backend.mockMvc.perform(delete("/api/module/delete").param("moduleId", idOf(PERSISTENCE)))
			.andExpect(status().isNoContent());

		recommendations().andExpect(jsonPath("$.merges", empty())).andExpect(jsonPath("$.moves", empty()));
	}

	// With SalesReport as the reference class, java.sql is permitted in the web module
	// and the controllers' dependency on the service layer is the violation.
	@Test
	void choosingAReferenceClassRecomputesTheViolations() throws Exception {
		String body = "{\"moduleId\":\"" + idOf(WEB) + "\",\"classIds\":[\"" + idOf(MISPLACED_CLASS) + "\"]}";
		backend.mockMvc.perform(post("/api/module/ref-clazzes").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk());

		recommendations().andExpect(jsonPath("$.violations[*].className",
				containsInAnyOrder("CatalogController", "CustomerController", "OrderController")));
	}

	@Test
	void aRejectedManualOperationRunsNoAnalysis() throws Exception {
		splitWeb("not-a-class-id").andExpect(status().isBadRequest());

		assertEquals(0, backend.analysisRunsSinceSetUp());
	}

	// No analysis reads the enforcement mode, so the recommendations cannot change.
	@Test
	void changingAnEnforcementModeRunsNoAnalysis() throws Exception {
		String body = "{\"moduleId\":\"" + idOf(WEB) + "\",\"classId\":\"" + idOf("CatalogController")
				+ "\",\"mode\":\"MUST\"}";
		backend.mockMvc
			.perform(post("/api/module/ref-clazz-mode").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk());

		assertEquals(0, backend.analysisRunsSinceSetUp());
	}

	private ResultActions recommendations() throws Exception {
		return backend.mockMvc.perform(get("/api/recommendations"));
	}

	private ResultActions splitWeb(String classId) throws Exception {
		String body = "{\"moduleId\":\"" + idOf(WEB) + "\",\"classIds\":[\"" + classId + "\"]}";
		return backend.mockMvc.perform(post("/api/module/split").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
