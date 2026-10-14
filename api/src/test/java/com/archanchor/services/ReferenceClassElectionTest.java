package com.archanchor.services;

import com.archanchor.domains.module.Module;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;
import com.archanchor.services.architecturalAnalyses.WeighedElectionFixture;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;

import static com.archanchor.services.architecturalAnalyses.WeighedElectionFixture.LEAVES_NO_VIOLATIONS;
import static com.archanchor.services.architecturalAnalyses.WeighedElectionFixture.MOST_ALIKE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The election takes its weights from the project's settings (#27), on the module of
 * {@link WeighedElectionFixture}, where the weights decide.
 */
class ReferenceClassElectionTest {

	private static final WeightPair SIMILARITY_FIRST = new WeightPair(0.9, 0.1);

	@Test
	void equalWeightsElectTheClassThatLeavesNoViolations() {
		assertThat(electedWith(RatingWeights.EQUAL)).isEqualTo(LEAVES_NO_VIOLATIONS);
	}

	@Test
	void similarityFirstElectionWeightsElectTheMostAlikeClass() {
		assertThat(electedWith(new RatingWeights(SIMILARITY_FIRST, WeightPair.EQUAL))).isEqualTo(MOST_ALIKE);
	}

	@Test
	void theRecommendationWeightsDoNotTakePartInTheElection() {
		assertThat(electedWith(new RatingWeights(WeightPair.EQUAL, SIMILARITY_FIRST))).isEqualTo(LEAVES_NO_VIOLATIONS);
	}

	private static String electedWith(RatingWeights weights) {
		ModuleService moduleService = new ModuleService(null, new ActiveRatingWeights(weights));

		Module web = WeighedElectionFixture.analysedWeb(moduleService).get(0);

		assertThat(web.getRefClazzes()).hasSize(1);
		return web.getRefClazzes().get(0).getName();
	}

}
