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
 * Regression suite for issue #44. A split is anchored on the reference class and on the
 * class least similar to it; a class with no dependencies, such as a marker interface, is
 * similar to nothing, so it was always that class, and every other class went to the
 * reference class's side.
 */
class SplitAnchorTest {

	private final ActiveRatingWeights activeWeights = new ActiveRatingWeights();

	private final ModuleService moduleService = new ModuleService(null, activeWeights);

	@Test
	void aClassWithoutDependenciesDoesNotAnchorASplit() {
		Module web = module(controller("CatalogController"), controller("CustomerController"),
				controller("OrderController"), clazz("SalesReport", "java.sql.Connection", "shop.model.Book"),
				clazz("Marker"));

		List<SplitModule.SplitModuleResult> splits = splitsOf(web, AnalysisSettings.DEFAULT);

		assertThat(splits).singleElement().satisfies(split -> {
			assertThat(split.deRefClassName).isEqualTo("SalesReport");
			assertThat(split.module2ClassNames).containsExactly("SalesReport");
		});
	}

	@Test
	void aModuleWhoseOtherClassesHaveNoDependenciesIsNotSplit() {
		Module web = module(controller("CatalogController"), clazz("MarkerA"), clazz("MarkerB"));

		assertThat(splitsOf(web, AnalysisSettings.DEFAULT)).isEmpty();
	}

	// SalesReport depends on two packages; with a minimum of three it is not judged
	// either.
	@Test
	void onlyClassesWithTheMinimumOfDependenciesAnchorASplit() {
		Module web = module(controller("CatalogController"), controller("CustomerController"),
				controller("OrderController"), clazz("SalesReport", "java.sql.Connection", "shop.model.Book"));

		assertThat(splitsOf(web, new AnalysisSettings(1, 1, null, 3, 0.0, null, null, null))).isEmpty();
	}

	private List<SplitModule.SplitModuleResult> splitsOf(Module module, AnalysisSettings settings) {
		SplitModule splitModule = new SplitModule(moduleService, activeWeights, new ActiveAnalysisSettings(settings));
		splitModule.execute(List.of(module));
		return splitModule.getResults();
	}

	private Module module(Clazz... clazzes) {
		Module module = new Module("shop/web", "shop/web", null, null, null, new ArrayList<>(List.of(clazzes)), 0.0,
				0.0, 0);
		AnalysedShopFixture.analyse(module, moduleService);
		return module;
	}

	private static Clazz controller(String name) {
		return clazz(name, "shop.service.ShopService", "shop.model.Book");
	}

	private static Clazz clazz(String name, String... usedTypes) {
		List<Dependency> dependencies = new ArrayList<>();
		for (String usedType : usedTypes) {
			Dependency dependency = new Dependency(usedType.substring(0, usedType.lastIndexOf('.')));
			dependency.addType(new Type(usedType));
			dependencies.add(dependency);
		}
		return new Clazz(name, name, dependencies, 0.0, 0.0, "shop/web", "shop/web", "ALLOW");
	}

}
