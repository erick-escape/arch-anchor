package com.archanchor.services.rating;

import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFileRatingWeightsStoreTest {

	private static final RatingWeights SIMILARITY_FIRST = new RatingWeights(new WeightPair(0.9, 0.1),
			new WeightPair(0.7, 0.3));

	@TempDir
	Path workspace;

	@Test
	void aProjectWithoutSettingsHasEqualWeights() {
		assertThat(storeIn("rating-weights.json").weightsOf("bookstore")).isEqualTo(RatingWeights.EQUAL);
	}

	@Test
	void savedWeightsOutliveTheStoreThatSavedThem() {
		storeIn("rating-weights.json").save("bookstore", SIMILARITY_FIRST);

		assertThat(storeIn("rating-weights.json").weightsOf("bookstore")).isEqualTo(SIMILARITY_FIRST);
	}

	@Test
	void eachProjectKeepsItsOwnWeights() {
		JsonFileRatingWeightsStore store = storeIn("rating-weights.json");

		store.save("bookstore", SIMILARITY_FIRST);
		store.save("shop", RatingWeights.EQUAL);

		assertThat(store.weightsOf("bookstore")).isEqualTo(SIMILARITY_FIRST);
		assertThat(store.weightsOf("shop")).isEqualTo(RatingWeights.EQUAL);
	}

	@Test
	void aForgottenProjectIsBackToEqualWeights() {
		JsonFileRatingWeightsStore store = storeIn("rating-weights.json");
		store.save("bookstore", SIMILARITY_FIRST);

		store.forget("bookstore");

		assertThat(storeIn("rating-weights.json").weightsOf("bookstore")).isEqualTo(RatingWeights.EQUAL);
	}

	@Test
	void anUnreadableFileIsReportedWithItsPath() throws IOException {
		Files.writeString(workspace.resolve("rating-weights.json"), "{ not json");

		assertThatThrownBy(() -> storeIn("rating-weights.json").weightsOf("bookstore"))
			.hasMessageContaining(workspace.resolve("rating-weights.json").toString())
			.hasMessageContaining("project name");
	}

	private JsonFileRatingWeightsStore storeIn(String fileName) {
		return new JsonFileRatingWeightsStore(workspace.resolve(fileName).toString());
	}

}
