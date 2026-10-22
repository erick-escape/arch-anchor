package com.archanchor.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPriorityTest {

	// The web's priorityOf (utils/priority.ts) reads the rate the API wrote, rounded to
	// four places (#71), so the summary must classify that rate too: 0.50003 is shown as
	// 0.5, which is not above 0.5.
	@ParameterizedTest
	@CsvSource({ "0.6, HIGH", "0.50005, HIGH", "0.50003, MEDIUM", "0.5, MEDIUM", "0.20004, LOW", "0.2, LOW",
			"0.01, LOW" })
	void classifiesTheRateAsTheApiWritesIt(double rate, RecommendationPriority priority) {
		assertThat(RecommendationPriority.of(rate)).isEqualTo(priority);
	}

}
