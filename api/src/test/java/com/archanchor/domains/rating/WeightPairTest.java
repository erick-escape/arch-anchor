package com.archanchor.domains.rating;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class WeightPairTest {

	@ParameterizedTest
	@CsvSource({ "0.5, 0.5", "0.0, 1.0", "1.0, 0.0", "0.7, 0.3", "0.9, 0.1" })
	void acceptsNonNegativeWeightsThatSumToOne(double similarity, double violation) {
		WeightPair pair = new WeightPair(similarity, violation);

		assertThat(pair.similarity()).isEqualTo(similarity);
		assertThat(pair.violation()).isEqualTo(violation);
	}

	@Test
	void rejectsANegativeWeightNamingItAndTheExpectedRange() {
		assertThatThrownBy(() -> new WeightPair(-0.2, 1.2)).isInstanceOf(InvalidRatingWeightsException.class)
			.hasMessageContaining("similarity=-0.2")
			.hasMessageContaining("violation=1.2")
			.hasMessageContaining("[0, 1]");
	}

	@Test
	void rejectsWeightsThatDoNotSumToOneNamingTheSum() {
		assertThatThrownBy(() -> new WeightPair(0.3, 0.6)).isInstanceOf(InvalidRatingWeightsException.class)
			.hasMessageContaining("similarity=0.3")
			.hasMessageContaining("violation=0.6")
			.hasMessageContaining("summing to 1")
			.hasMessageContaining("sum 0.9");
	}

	@Test
	void rejectsAWeightThatIsNotANumber() {
		assertThatThrownBy(() -> new WeightPair(Double.NaN, 1.0)).isInstanceOf(InvalidRatingWeightsException.class)
			.hasMessageContaining("similarity=NaN");
	}

	@Test
	void equalWeightsAreTheDefaultOfTheEcsaRatingMechanism() {
		assertThat(WeightPair.EQUAL).isEqualTo(new WeightPair(0.5, 0.5));
	}

	@Test
	void ratesASimilarityTermAndAViolationTermByTheirWeights() {
		WeightPair similarityFirst = new WeightPair(0.8, 0.2);

		assertThat(similarityFirst.rate(0.25, 1.0)).isCloseTo(0.4, within(1e-12));
	}

}
