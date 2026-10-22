package com.archanchor.util;

import com.archanchor.dto.recommendations.MergeMetricsDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression suite for #71: the API wrote every double at full precision. */
class FourDecimalPlacesTest {

	private final ObjectMapper mapper = new ObjectMapper().registerModule(FourDecimalPlaces.jacksonModule());

	@ParameterizedTest
	@CsvSource({ "0.4444444444444444, 0.4444", "0.5444699546485261, 0.5445", "0.00005, 0.0001", "0.00004999, 0.0",
			"-0.06920000001, -0.0692", "1.0, 1.0", "0.0, 0.0" })
	void roundsHalfUpToFourDecimalPlaces(double value, double rounded) {
		assertThat(FourDecimalPlaces.round(value)).isEqualTo(rounded);
	}

	@Test
	void leavesValuesThatAreNotFiniteAlone() {
		assertThat(FourDecimalPlaces.round(Double.NaN)).isNaN();
		assertThat(FourDecimalPlaces.round(Double.POSITIVE_INFINITY)).isEqualTo(Double.POSITIVE_INFINITY);
	}

	@Test
	void writesPrimitiveDoublesWithAtMostFourDecimalPlacesAndLeavesIntegersAlone() throws Exception {
		MergeMetricsDTO merge = new MergeMetricsDTO(0.4444444444444444, 0.7865646258503401, 0.7044444444444444, 1, 0, 0,
				0.0889399092970522, 1.0, 0.5444699546485261);

		assertThat(mapper.writeValueAsString(merge)).isEqualTo("{\"module1Similarity\":0.4444,"
				+ "\"module2Similarity\":0.7866,\"mergedSimilarity\":0.7044,\"module1Violations\":1,"
				+ "\"module2Violations\":0,\"mergedViolations\":0,\"similarityImprovement\":0.0889,"
				+ "\"violationsImprovement\":1.0,\"rate\":0.5445}");
	}

	@Test
	void writesBoxedDoublesTheSameWayAndNullAsNull() throws Exception {
		List<Double> values = Arrays.asList(0.33080808080808083, null);

		assertThat(mapper.writeValueAsString(values)).isEqualTo("[0.3308,null]");
	}

	// Double.toString, which Jackson uses for a double, would write 1.0E-4
	@Test
	void writesPlainDecimalsWithAtLeastOneDecimalPlace() throws Exception {
		List<Double> values = List.of(0.00012, 100.0, 0.5000001, Double.NaN);

		assertThat(mapper.writeValueAsString(values)).isEqualTo("[0.0001,100.0,0.5,\"NaN\"]");
	}

}
