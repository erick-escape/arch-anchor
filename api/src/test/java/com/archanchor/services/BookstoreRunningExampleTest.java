package com.archanchor.services;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.dto.recommendations.RecommendationsResponseDTO;
import com.archanchor.dto.recommendations.RecommendationsSummaryDTO;
import com.archanchor.services.analysis.InMemoryAnalysisSettingsStore;
import com.archanchor.services.rating.InMemoryRatingWeightsStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * {@code examples/bookstore} analysed end to end, with the real parser. Its README and
 * the ICSE 2027 demo paper quote these results, so a failure here means both are stale,
 * not just this test.
 */
class BookstoreRunningExampleTest {

	private static final String BOOKSTORE = Path.of("..", "examples", "bookstore").toString();

	private static final double QUOTED_PRECISION = 1e-4;

	private static final RatingWeights SIMILARITY_FIRST_RECOMMENDATIONS = new RatingWeights(WeightPair.EQUAL,
			new WeightPair(0.9, 0.1));

	private final HandWiredAnalysis analysis = new HandWiredAnalysis();

	private final InMemoryRatingWeightsStore ratingWeightsStore = analysis.ratingWeightsStore;

	private final InMemoryAnalysisSettingsStore analysisSettingsStore = analysis.analysisSettingsStore;

	private final ProjectService projectService = analysis.projectService;

	private final RecommendationsService recommendationsService = analysis.recommendationsService;

	@Test
	void electsTheReferenceClassesTheReadmeQuotes() throws IOException {
		ProjectAnalysesDTO analyses = projectService.analyzeProject("bookstore", BOOKSTORE);

		assertThat(analyses.modulesList()).extracting(ModuleDTO::name, module -> module.refClazzes()[0].name())
			.containsExactly(tuple("bookstore/model", "Book"), tuple("bookstore/persistence", "BookRepository"),
					tuple("bookstore/service", "CatalogService"), tuple("bookstore/web", "CatalogController"));
		assertThat(analyses.projectSimilarity()).isCloseTo(0.9375, within(QUOTED_PRECISION));
	}

	@Test
	void ratesTheThreeRefactoringsTheReadmeQuotes() throws IOException {
		projectService.analyzeProject("bookstore", BOOKSTORE);

		RecommendationsResponseDTO recommendations = recommendationsService.getRecommendations();

		assertThat(recommendations.splits()).singleElement().satisfies(split -> {
			assertThat(split.originalModuleName()).isEqualTo("bookstore/web");
			assertThat(split.metrics().rate()).isCloseTo(0.625, within(QUOTED_PRECISION));
		});
		assertThat(recommendations.moves()).singleElement().satisfies(move -> {
			assertThat(move.className()).isEqualTo("SalesReport");
			assertThat(move.targetModuleName()).isEqualTo("bookstore/persistence");
			assertThat(move.metrics().rate()).isCloseTo(0.5625, within(QUOTED_PRECISION));
		});
		assertThat(recommendations.merges()).singleElement()
			.satisfies(merge -> assertThat(merge.metrics().rate()).isCloseTo(0.4196, within(QUOTED_PRECISION)));
		assertThat(recommendations.summary()).isEqualTo(new RecommendationsSummaryDTO(4, 2, 2, 0));
	}

	// Every refactoring the bookstore is offered removes its only violation, so the
	// weights cannot reorder split, move and merge; they change what each is worth, and
	// whether the merge, which costs similarity, is worth making at all.
	@Test
	void similarityFirstRecommendationWeightsRerateTheRefactoringsAndDropTheMerge() throws IOException {
		ratingWeightsStore.save("bookstore", SIMILARITY_FIRST_RECOMMENDATIONS);

		projectService.analyzeProject("bookstore", BOOKSTORE);

		assertSimilarityFirstRecommendations();
	}

	@Test
	void changingTheWeightsOfTheAnalysedBookstoreReratesItsRefactorings() throws IOException {
		projectService.analyzeProject("bookstore", BOOKSTORE);

		projectService.updateRatingWeights("bookstore", SIMILARITY_FIRST_RECOMMENDATIONS);

		assertSimilarityFirstRecommendations();
	}

	// The merge is rated 0.4196, the split and the move above 0.5; the violation has no
	// rate and stays (#36).
	@Test
	void aMinimumRateSavedBeforeTheAnalysisHidesTheMerge() throws IOException {
		analysisSettingsStore.save("bookstore", new AnalysisSettings(1, 1, null, 0, 0.5, null, null, null));

		projectService.analyzeProject("bookstore", BOOKSTORE);

		RecommendationsResponseDTO recommendations = recommendationsService.getRecommendations();
		assertThat(recommendations.merges()).isEmpty();
		assertThat(recommendations.summary()).isEqualTo(new RecommendationsSummaryDTO(3, 2, 1, 0));
	}

	private void assertSimilarityFirstRecommendations() {
		RecommendationsResponseDTO recommendations = recommendationsService.getRecommendations();

		assertThat(recommendations.splits()).singleElement()
			.satisfies(split -> assertThat(split.metrics().rate()).isCloseTo(0.325, within(QUOTED_PRECISION)));
		assertThat(recommendations.moves()).singleElement()
			.satisfies(move -> assertThat(move.metrics().rate()).isCloseTo(0.2125, within(QUOTED_PRECISION)));
		assertThat(recommendations.merges()).isEmpty();
		assertThat(recommendations.summary()).isEqualTo(new RecommendationsSummaryDTO(3, 0, 3, 0));
	}

}
