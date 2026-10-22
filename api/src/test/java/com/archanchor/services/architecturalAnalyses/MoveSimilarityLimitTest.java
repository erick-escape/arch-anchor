package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.analysis.AnalysisSettings;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;
import com.archanchor.services.analysis.ActiveAnalysisSettings;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #73, on the pull of small modules seen in the ICSE 2027 evaluation: SalesReport's best
 * target is a one-class ledger that accepts all its dependencies but loses similarity by
 * taking it (rate 0.48, similarity -0.04). The persistence module gains similarity but
 * keeps one of its violations (rate 0.26, similarity +0.03).
 */
class MoveSimilarityLimitTest {

	private static final String LEDGER = "shop/ledger";

	private static final String PERSISTENCE = "shop/persistence";

	private final ActiveRatingWeights activeWeights = new ActiveRatingWeights();

	private final ModuleService moduleService = new ModuleService(null, activeWeights);

	@Test
	void withoutALimitTheBestTargetIsTheOneThatCostsSimilarity() {
		assertThat(targetsOfSalesReport(null, 1)).containsExactly(LEDGER);
	}

	// Applied before each class's best targets are kept: applied after, SalesReport would
	// get no card at all.
	@Test
	void aClassWhoseBestTargetCostsSimilarityKeepsItsBestTargetThatDoesNot() {
		assertThat(targetsOfSalesReport(0.0, 1)).containsExactly(PERSISTENCE);
	}

	@Test
	void aNegativeLimitToleratesSomeLoss() {
		assertThat(targetsOfSalesReport(-0.05, null)).containsExactly(LEDGER, PERSISTENCE);
		assertThat(targetsOfSalesReport(-0.04, null)).containsExactly(PERSISTENCE);
	}

	private List<String> targetsOfSalesReport(Double minMoveSimilarityChange, Integer maxMoveTargetsPerClass) {
		ActiveAnalysisSettings activeSettings = new ActiveAnalysisSettings();
		activeSettings.adopt("shop",
				new AnalysisSettings(1, 1, maxMoveTargetsPerClass, 0, 0.0, null, minMoveSimilarityChange, null));
		MoveClass moveClass = new MoveClass(moduleService, activeWeights, activeSettings);

		moveClass.execute(List.of(web(), ledger(), persistence()));

		return moveClass.getResults()
			.stream()
			.filter(move -> move.className.equals("SalesReport"))
			.map(move -> move.targetModuleName)
			.toList();
	}

	private Module web() {
		List<Clazz> classes = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			classes.add(clazz("Controller" + i, "shop.web", "shop.service.ShopService", "shop.model.Book"));
		}
		classes.add(clazz("SalesReport", "shop.web", "java.sql.Connection", "shop.model.Book", "java.time.LocalDate"));
		return module("shop/web", classes);
	}

	private Module ledger() {
		return module(LEDGER, List.of(clazz("Ledger", "shop.ledger", "java.sql.Connection", "shop.model.Book",
				"java.time.LocalDate", "java.io.File", "java.net.URI")));
	}

	private Module persistence() {
		List<Clazz> classes = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			classes.add(clazz("Repository" + i, "shop.persistence", "java.sql.Connection", "shop.model.Book",
					"javax.sql.DataSource"));
		}
		return module(PERSISTENCE, classes);
	}

	private Module module(String name, List<Clazz> clazzes) {
		Module module = new Module(name, name, null, null, null, new ArrayList<>(clazzes), 0.0, 0.0, 0);
		AnalysedShopFixture.analyse(module, moduleService);
		return module;
	}

	private static Clazz clazz(String name, String packageName, String... usedTypes) {
		List<Dependency> dependencies = new ArrayList<>();
		for (String usedType : usedTypes) {
			Dependency dependency = new Dependency(usedType.substring(0, usedType.lastIndexOf('.')));
			dependency.addType(new Type(usedType));
			dependencies.add(dependency);
		}
		Clazz clazz = new Clazz(name, name, dependencies, 0.0, 0.0, name, name, "ALLOW");
		clazz.setPackageName(packageName);
		return clazz;
	}

}
