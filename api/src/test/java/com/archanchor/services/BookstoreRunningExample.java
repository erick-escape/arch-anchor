package com.archanchor.services;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.dto.recommendations.RecommendationsResponseDTO;
import com.archanchor.dto.recommendations.RecommendationsSummaryDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * The bookstore analysed end to end with the real parsers, in whichever language it is
 * written (#85). Every {@code examples/bookstore*} is the same architecture: four layers,
 * thirteen classes of the same names, one class in the wrong layer. A parser that reads
 * its language well sees the same dependency sets in each, so all of them are held to the
 * same numbers here, and only the names of the project and of its modules differ.
 * <p>
 * A language's test extends this and says what its example is called:
 *
 * <pre>
 * BookstoreRunningExampleTest() {
 * 	super("bookstore", new Layers("bookstore/model", "bookstore/persistence", "bookstore/service", "bookstore/web"));
 * }
 * </pre>
 */
abstract class BookstoreRunningExample {

	private static final double QUOTED_PRECISION = 1e-4;

	private static final RatingWeights SIMILARITY_FIRST_RECOMMENDATIONS = new RatingWeights(WeightPair.EQUAL,
			new WeightPair(0.9, 0.1));

	private final String projectName;

	private final Layers layers;

	private final HandWiredAnalysis analysis = new HandWiredAnalysis();

	/**
	 * The name of the example's directory under {@code examples/}, and of its modules.
	 */
	BookstoreRunningExample(String exampleDirectory, Layers layers) {
		this.projectName = exampleDirectory;
		this.layers = layers;
	}

	/** The names the tool gives the modules of the bookstore's four layers. */
	record Layers(String model, String persistence, String service, String web) {
	}

	/** Where an example is, seen from where the tests run. */
	static String directoryOf(String exampleDirectory) {
		return Path.of("..", "examples", exampleDirectory).toString();
	}

	/** The example analysed with the weights and settings saved so far. */
	final ProjectAnalysesDTO analysed() throws IOException {
		return analysis.projectService.analyzeProject(projectName, directoryOf(projectName));
	}

	final RecommendationsResponseDTO recommendations() {
		return analysis.recommendationsService.getRecommendations();
	}

	@Test
	void electsTheReferenceClassesTheReadmeQuotes() throws IOException {
		ProjectAnalysesDTO analyses = analysed();

		assertThat(analyses.modulesList()).extracting(ModuleDTO::name, module -> module.refClazzes()[0].name())
			.containsExactly(tuple(layers.model(), "Book"), tuple(layers.persistence(), "BookRepository"),
					tuple(layers.service(), "CatalogService"), tuple(layers.web(), "CatalogController"));
		assertThat(analyses.projectSimilarity()).isCloseTo(0.9375, within(QUOTED_PRECISION));
	}

	@Test
	void ratesTheThreeRefactoringsTheReadmeQuotes() throws IOException {
		analysed();

		RecommendationsResponseDTO recommendations = recommendations();

		assertThat(recommendations.splits()).singleElement().satisfies(split -> {
			assertThat(split.originalModuleName()).isEqualTo(layers.web());
			assertThat(split.metrics().rate()).isCloseTo(0.625, within(QUOTED_PRECISION));
		});
		assertThat(recommendations.moves()).singleElement().satisfies(move -> {
			assertThat(move.className()).isEqualTo("SalesReport");
			assertThat(move.targetModuleName()).isEqualTo(layers.persistence());
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
		analysis.ratingWeightsStore.save(projectName, SIMILARITY_FIRST_RECOMMENDATIONS);

		analysed();

		assertSimilarityFirstRecommendations();
	}

	@Test
	void changingTheWeightsOfTheAnalysedBookstoreReratesItsRefactorings() throws IOException {
		analysed();

		analysis.projectService.updateRatingWeights(projectName, SIMILARITY_FIRST_RECOMMENDATIONS);

		assertSimilarityFirstRecommendations();
	}

	// The merge is rated 0.4196, the split and the move above 0.5; the violation has no
	// rate and stays (#36).
	@Test
	void aMinimumRateSavedBeforeTheAnalysisHidesTheMerge() throws IOException {
		analysis.analysisSettingsStore.save(projectName, new AnalysisSettings(1, 1, null, 0, 0.5, null, null, null));

		analysed();

		RecommendationsResponseDTO recommendations = recommendations();
		assertThat(recommendations.merges()).isEmpty();
		assertThat(recommendations.summary()).isEqualTo(new RecommendationsSummaryDTO(3, 2, 1, 0));
	}

	private void assertSimilarityFirstRecommendations() {
		RecommendationsResponseDTO recommendations = recommendations();

		assertThat(recommendations.splits()).singleElement()
			.satisfies(split -> assertThat(split.metrics().rate()).isCloseTo(0.325, within(QUOTED_PRECISION)));
		assertThat(recommendations.moves()).singleElement()
			.satisfies(move -> assertThat(move.metrics().rate()).isCloseTo(0.2125, within(QUOTED_PRECISION)));
		assertThat(recommendations.merges()).isEmpty();
		assertThat(recommendations.summary()).isEqualTo(new RecommendationsSummaryDTO(3, 0, 3, 0));
	}

}
