package com.archanchor.domains.rating;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingWeightsTest {

	@Test
	void defaultsToEqualWeightsForBothTheElectionAndTheRecommendationRate() {
		assertThat(RatingWeights.EQUAL.election()).isEqualTo(WeightPair.EQUAL);
		assertThat(RatingWeights.EQUAL.recommendation()).isEqualTo(WeightPair.EQUAL);
	}

	@Test
	void keepsTheTwoPairsApart() {
		RatingWeights weights = new RatingWeights(new WeightPair(0.9, 0.1), new WeightPair(0.2, 0.8));

		assertThat(weights.election()).isEqualTo(new WeightPair(0.9, 0.1));
		assertThat(weights.recommendation()).isEqualTo(new WeightPair(0.2, 0.8));
	}

	@Test
	void rejectsAMissingElectionPairNamingIt() {
		assertThatThrownBy(() -> new RatingWeights(null, WeightPair.EQUAL))
			.isInstanceOf(InvalidRatingWeightsException.class)
			.hasMessageContaining("election");
	}

	@Test
	void rejectsAMissingRecommendationPairNamingIt() {
		assertThatThrownBy(() -> new RatingWeights(WeightPair.EQUAL, null))
			.isInstanceOf(InvalidRatingWeightsException.class)
			.hasMessageContaining("recommendation");
	}

}
