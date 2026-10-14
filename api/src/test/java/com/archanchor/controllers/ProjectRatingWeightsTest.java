package com.archanchor.controllers;

import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.services.architecturalAnalyses.WeighedElectionFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.archanchor.controllers.ShopBackend.PROJECT;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.idOf;
import static com.archanchor.services.architecturalAnalyses.WeighedElectionFixture.LEAVES_NO_VIOLATIONS;
import static com.archanchor.services.architecturalAnalyses.WeighedElectionFixture.MOST_ALIKE;
import static com.archanchor.services.architecturalAnalyses.WeighedElectionFixture.OTHER_CONTROLLER;
import static com.archanchor.services.architecturalAnalyses.WeighedElectionFixture.WEB;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The architect sets a project's election and recommendation weights through the API
 * (#27), on the module of {@link WeighedElectionFixture}, whose election the weights
 * decide.
 */
class ProjectRatingWeightsTest {

	private static final String OTHER_PROJECT = "bookstore";

	private static final String EQUAL = "{\"similarity\":0.5,\"violation\":0.5}";

	private static final String SIMILARITY_FIRST = "{\"similarity\":0.9,\"violation\":0.1}";

	private static final String NOT_SUMMING_TO_ONE = "{\"similarity\":0.3,\"violation\":0.6}";

	@TempDir
	Path projectsRoot;

	private ShopBackend backend;

	@BeforeEach
	void analyseTheShop() throws IOException {
		Files.createDirectories(projectsRoot.resolve(PROJECT));
		Files.createDirectories(projectsRoot.resolve(OTHER_PROJECT));
		backend = ShopBackend.analysed(WeighedElectionFixture::analysedWeb, projectsRoot);
	}

	@Test
	void aProjectStartsWithEqualWeights() throws Exception {
		ratingWeightsOf(PROJECT).andExpect(status().isOk())
			.andExpect(jsonPath("$.election.similarity").value(0.5))
			.andExpect(jsonPath("$.election.violation").value(0.5))
			.andExpect(jsonPath("$.recommendation.similarity").value(0.5))
			.andExpect(jsonPath("$.recommendation.violation").value(0.5));
	}

	@Test
	void savedWeightsAreTheProjectsFromThenOn() throws Exception {
		setRatingWeights(PROJECT, weights(SIMILARITY_FIRST, "{\"similarity\":0.2,\"violation\":0.8}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.recommendation.violation").value(0.8));

		ratingWeightsOf(PROJECT).andExpect(jsonPath("$.election.similarity").value(0.9))
			.andExpect(jsonPath("$.recommendation.similarity").value(0.2));
	}

	@Test
	void aNegativeWeightIsRejectedNamingTheValueAndTheExpectedRange() throws Exception {
		setRatingWeights(PROJECT, weights("{\"similarity\":-0.2,\"violation\":1.2}", EQUAL))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail",
					allOf(containsString("election"), containsString("similarity=-0.2"), containsString("[0, 1]"))));
	}

	@Test
	void weightsThatDoNotSumToOneAreRejectedNamingTheSum() throws Exception {
		setRatingWeights(PROJECT, weights(EQUAL, NOT_SUMMING_TO_ONE)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", allOf(containsString("recommendation"), containsString("sum 0.9"))));
	}

	@Test
	void aMissingPairIsRejectedNamingIt() throws Exception {
		setRatingWeights(PROJECT, "{\"election\":" + EQUAL + "}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", containsString("recommendation")));
	}

	// A missing weight read as 0 would pass as (1, 0) when the other one is 1.
	@Test
	void aMissingWeightIsRejectedRatherThanReadAsZero() throws Exception {
		setRatingWeights(PROJECT, weights("{\"similarity\":1.0}", EQUAL)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", allOf(containsString("election"), containsString("violation=null"))));
	}

	@Test
	void aRejectedChangeKeepsTheWeightsAndRunsNothing() throws Exception {
		setRatingWeights(PROJECT, weights(EQUAL, NOT_SUMMING_TO_ONE)).andExpect(status().isBadRequest());

		ratingWeightsOf(PROJECT).andExpect(jsonPath("$.recommendation.similarity").value(0.5));
		assertEquals(0, backend.analysisRunsSinceSetUp());
	}

	@Test
	void anUnknownProjectIsNotFound() throws Exception {
		ratingWeightsOf("no-such-project").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail", containsString("no-such-project")));
		setRatingWeights("no-such-project", weights(EQUAL, EQUAL)).andExpect(status().isNotFound());
	}

	@Test
	void newElectionWeightsReElectTheSavedModulesAndRunTheAnalysesOnce() throws Exception {
		referenceClassesOfWeb().andExpect(jsonPath("$[0].refClazzes[*].name", contains(LEAVES_NO_VIOLATIONS)));

		setRatingWeights(PROJECT, weights(SIMILARITY_FIRST, EQUAL)).andExpect(status().isOk());

		referenceClassesOfWeb().andExpect(jsonPath("$[0].refClazzes[*].name", contains(MOST_ALIKE)));
		assertEquals(1, backend.analysisRunsSinceSetUp());
	}

	@Test
	void newRecommendationWeightsRunTheAnalysesOnce() throws Exception {
		setRatingWeights(PROJECT, weights(EQUAL, SIMILARITY_FIRST)).andExpect(status().isOk());

		referenceClassesOfWeb().andExpect(jsonPath("$[0].refClazzes[*].name", contains(LEAVES_NO_VIOLATIONS)));
		assertEquals(1, backend.analysisRunsSinceSetUp());
	}

	@Test
	void newElectionWeightsKeepReferenceClassesChosenByHand() throws Exception {
		chooseReferenceClassOfWeb(OTHER_CONTROLLER).andExpect(status().isOk());

		setRatingWeights(PROJECT, weights(SIMILARITY_FIRST, EQUAL)).andExpect(status().isOk());

		referenceClassesOfWeb().andExpect(jsonPath("$[0].refClazzes[*].name", contains(OTHER_CONTROLLER)))
			.andExpect(jsonPath("$[0].refClazzesChosenByHand").value(true));
	}

	// The saved modules are the shop's; the bookstore's weights apply when it is next
	// analysed.
	@Test
	void weightsOfAProjectOtherThanTheAnalysedOneAreOnlyStored() throws Exception {
		setRatingWeights(OTHER_PROJECT, weights(SIMILARITY_FIRST, EQUAL)).andExpect(status().isOk());

		ratingWeightsOf(OTHER_PROJECT).andExpect(jsonPath("$.election.similarity").value(0.9));
		referenceClassesOfWeb().andExpect(jsonPath("$[0].refClazzes[*].name", contains(LEAVES_NO_VIOLATIONS)));
		assertEquals(0, backend.analysisRunsSinceSetUp());
	}

	// Otherwise a new project of the same name would start from the old one's weights.
	@Test
	void deletingAProjectForgetsItsWeights() throws Exception {
		setRatingWeights(OTHER_PROJECT, weights(SIMILARITY_FIRST, EQUAL)).andExpect(status().isOk());

		backend.mockMvc.perform(delete("/api/projects/" + OTHER_PROJECT)).andExpect(status().isOk());

		assertEquals(RatingWeights.EQUAL, backend.ratingWeightsStore.weightsOf(OTHER_PROJECT));
	}

	private ResultActions ratingWeightsOf(String projectName) throws Exception {
		return backend.mockMvc.perform(get("/api/projects/" + projectName + "/rating-weights"));
	}

	private ResultActions setRatingWeights(String projectName, String body) throws Exception {
		return backend.mockMvc
			.perform(put("/api/projects/" + projectName + "/rating-weights").contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions referenceClassesOfWeb() throws Exception {
		return backend.mockMvc.perform(get("/api/module")).andExpect(jsonPath("$[0].name").value(WEB));
	}

	private ResultActions chooseReferenceClassOfWeb(String className) throws Exception {
		String body = "{\"moduleId\":\"" + idOf(WEB) + "\",\"classIds\":[\"" + idOf(className) + "\"]}";
		return backend.mockMvc
			.perform(post("/api/module/ref-clazzes").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String weights(String election, String recommendation) {
		return "{\"election\":" + election + ",\"recommendation\":" + recommendation + "}";
	}

}
