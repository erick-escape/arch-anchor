package com.archanchor.controllers;

import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.archanchor.controllers.ShopBackend.PROJECT;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.PERSISTENCE;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.WEB;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.idOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression suite for issue #34. A merge card is rated for a module that keeps the
 * reference classes of both modules, so it allows what either allowed; applying the card
 * elected one reference class instead, and built a module with the violations the card
 * said would go. In the shop, the web module's {@code java.sql} is allowed by the
 * persistence module's reference class.
 */
class AppliedMergeKeepsReferenceClassesTest {

	@TempDir
	Path projectsRoot;

	private ShopBackend backend;

	private String card;

	@BeforeEach
	void analyseTheShop() throws Exception {
		Files.createDirectories(projectsRoot.resolve(PROJECT));
		backend = ShopBackend.analysed(AnalysedShopFixture::webAndPersistence, projectsRoot);
		card = backend.mockMvc.perform(get("/api/recommendations")).andReturn().getResponse().getContentAsString();
	}

	@Test
	void anAppliedMergeKeepsTheReferenceClassesOfBothModules() throws Exception {
		Set<String> referenceClassesBefore = referenceClassIdsOf(savedModules(), WEB);
		referenceClassesBefore.addAll(referenceClassIdsOf(savedModules(), PERSISTENCE));

		String merged = applyTheMergeCard();

		assertThat(JsonPath.<List<String>>read(merged, "$.refClazzes[*].id"))
			.containsExactlyInAnyOrderElementsOf(referenceClassesBefore);
	}

	@Test
	void anAppliedMergeHasTheViolationsItsCardShowed() throws Exception {
		String merged = applyTheMergeCard();

		assertThat(violationsOf(merged))
			.isEqualTo(JsonPath.<Integer>read(card, "$.merges[0].metrics.mergedViolations"));
	}

	// Re-election skips modules whose reference classes were chosen by hand; otherwise
	// the next change of weights would elect one of the two.
	@Test
	void changingTheWeightsKeepsBothReferenceClassesOfAMergedModule() throws Exception {
		String merged = applyTheMergeCard();
		String mergedId = JsonPath.read(merged, "$.id");

		backend.mockMvc
			.perform(put("/api/projects/{projectName}/rating-weights", PROJECT).contentType(MediaType.APPLICATION_JSON)
				.content("{\"election\":{\"similarity\":0.9,\"violation\":0.1},"
						+ "\"recommendation\":{\"similarity\":0.5,\"violation\":0.5}}"))
			.andExpect(status().isOk());

		assertThat(JsonPath.<List<Object>>read(savedModules(), "$[?(@.id == '" + mergedId + "')].refClazzes[*]"))
			.hasSize(2);
	}

	// Dragging one module onto another merges through the same endpoint the card uses.
	@Test
	void aMergeByHandKeepsTheReferenceClassesOfBothModulesToo() throws Exception {
		String merged = backend.mockMvc
			.perform(post("/api/module/merge").param("sourceId", idOf(WEB)).param("targetId", idOf(PERSISTENCE)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(JsonPath.<List<Object>>read(merged, "$.refClazzes[*]")).hasSize(2);
	}

	private String applyTheMergeCard() throws Exception {
		return backend.mockMvc
			.perform(post("/api/recommendations/apply/merge")
				.param("sourceId", JsonPath.<String>read(card, "$.merges[0].module1Id"))
				.param("targetId", JsonPath.<String>read(card, "$.merges[0].module2Id")))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private String savedModules() throws Exception {
		return backend.mockMvc.perform(get("/api/module")).andReturn().getResponse().getContentAsString();
	}

	private static Set<String> referenceClassIdsOf(String modules, String moduleName) {
		return new HashSet<>(JsonPath.read(modules, "$[?(@.name == '" + moduleName + "')].refClazzes[*].id"));
	}

	// The module DTO carries no violation count; it is |D(m) \ R(m)| by definition.
	private static int violationsOf(String module) {
		Set<String> violating = new HashSet<>(JsonPath.read(module, "$.allDependencies[*].packageName"));
		violating.removeAll(JsonPath.<List<String>>read(module, "$.refClazzesDependencies[*].packageName"));
		return violating.size();
	}

}
