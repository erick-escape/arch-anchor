package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.module.Module;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The election takes its weights from the project's settings (#27). The module is built
 * so that the weights decide: the two controllers are the most alike, but either one as
 * the reference class leaves two of the module's dependencies as violations, whereas the
 * report uses every dependency of the module and leaves none.
 */
class ReferenceClassElectionTest {

	private static final WeightPair SIMILARITY_FIRST = new WeightPair(0.9, 0.1);

	@Test
	void equalWeightsElectTheClassThatLeavesNoViolations() {
		assertThat(electedWith(RatingWeights.EQUAL)).isEqualTo("SalesReport");
	}

	@Test
	void similarityFirstElectionWeightsElectTheMostAlikeClass() {
		assertThat(electedWith(new RatingWeights(SIMILARITY_FIRST, WeightPair.EQUAL))).isEqualTo("CatalogController");
	}

	@Test
	void theRecommendationWeightsDoNotTakePartInTheElection() {
		assertThat(electedWith(new RatingWeights(WeightPair.EQUAL, SIMILARITY_FIRST))).isEqualTo("SalesReport");
	}

	private static String electedWith(RatingWeights weights) {
		ModuleService moduleService = new ModuleService(null, new ActiveRatingWeights(weights));
		Module web = webModule();

		moduleService.calculateClassSimilaritiesAndSelectRefClasses(web);

		assertThat(web.getRefClazzes()).hasSize(1);
		return web.getRefClazzes().get(0).getName();
	}

	private static Module webModule() {
		List<Clazz> clazzes = List.of(clazzOf("CatalogController", "shop.service", "shop.model"),
				clazzOf("OrderController", "shop.service", "shop.model"),
				clazzOf("SalesReport", "shop.service", "shop.model", "java.sql", "java.time"));
		return new Module("web", "shop/web", null, null, null, new ArrayList<>(clazzes), 0.0, 0.0, 0);
	}

	private static Clazz clazzOf(String name, String... packageNames) {
		List<Dependency> dependencies = new ArrayList<>(Arrays.stream(packageNames).map(Dependency::new).toList());
		return new Clazz(name, name, dependencies, 0.0, 0.0, "shop/web", "shop/web", "ALLOW");
	}

}
