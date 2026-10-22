package com.archanchor.services;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.domains.rating.WeightPair;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.dto.recommendations.RecommendationsResponseDTO;
import com.archanchor.dto.recommendations.RecommendationsSummaryDTO;
import com.archanchor.services.analysis.ActiveAnalysisSettings;
import com.archanchor.services.analysis.InMemoryAnalysisSettingsStore;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import com.archanchor.services.architecturalAnalyses.ArchitectureViolation;
import com.archanchor.services.architecturalAnalyses.MergeModule;
import com.archanchor.services.architecturalAnalyses.MoveClass;
import com.archanchor.services.architecturalAnalyses.SplitModule;
import com.archanchor.services.parsers.JavaParser;
import com.archanchor.services.parsers.ParserFactory;
import com.archanchor.services.rating.ActiveRatingWeights;
import com.archanchor.services.rating.InMemoryRatingWeightsStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

	private final ParserFactory parserFactory = javaParserFactory();

	private final ActiveRatingWeights activeWeights = new ActiveRatingWeights();

	private final InMemoryRatingWeightsStore ratingWeightsStore = new InMemoryRatingWeightsStore();

	private final ActiveAnalysisSettings activeSettings = new ActiveAnalysisSettings();

	private final InMemoryAnalysisSettingsStore analysisSettingsStore = new InMemoryAnalysisSettingsStore();

	private final InMemoryModuleService moduleService = new InMemoryModuleService(parserFactory, activeWeights);

	private final SplitModule splitModule = new SplitModule(moduleService, activeWeights, activeSettings);

	private final MergeModule mergeModule = new MergeModule(moduleService, activeWeights, activeSettings);

	private final MoveClass moveClass = new MoveClass(moduleService, activeWeights, activeSettings);

	private final ArchitectureViolation architectureViolation = new ArchitectureViolation(moduleService, activeWeights,
			activeSettings);

	// Same order as the running application: Spring registers the analyses by class name.
	private final ProjectService projectService = new ProjectService(moduleService, parserFactory,
			new ArchitecturalAnalysesRunner(List.of(architectureViolation, mergeModule, moveClass, splitModule)),
			ratingWeightsStore, activeWeights, analysisSettingsStore, activeSettings);

	private final RecommendationsService recommendationsService = new RecommendationsService(splitModule, mergeModule,
			moveClass, architectureViolation, activeSettings);

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

	// The bookstore declares no dependencies, so the parser resolves everything from the
	// JDK and downloads nothing.
	private static ParserFactory javaParserFactory() {
		JavaParserService javaParserService = new JavaParserService();
		ParserFactory factory = new ParserFactory(javaParserService);
		factory.registerParser(new JavaParser(javaParserService));
		return factory;
	}

}
