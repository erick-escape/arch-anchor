package com.archanchor.services.architecturalAnalyses;

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
 * Regression suite for issue #43, on the move the ICSE 2027 evaluation seeds: a class
 * moved out of its package imports the classes it left behind. Moving it back must not
 * count those imports as a dependency of its old module on itself.
 */
class MovingAClassBackTest {

	private final ActiveRatingWeights activeWeights = new ActiveRatingWeights();

	private final ModuleService moduleService = new ModuleService(null, activeWeights);

	@Test
	void movingAMisplacedClassBackAddsNoViolationToItsHome() {
		Module web = module("shop/web",
				clazz("CatalogController", "shop.web", "shop.service.ShopService", "shop.model.Book"),
				clazz("CustomerController", "shop.web", "shop.service.ShopService", "shop.model.Book"),
				clazz("OrderController", "shop.web", "shop.service.ShopService", "shop.model.Book"),
				clazz("OrderService", "shop.web", "shop.orders.Order", "shop.model.Book"));
		Module orders = module("shop/orders", clazz("Order", "shop.orders", "shop.model.Book"),
				clazz("OrderRepository", "shop.orders", "shop.model.Book", "java.sql.Connection"));
		MoveClass moveClass = new MoveClass(moduleService, activeWeights, new ActiveAnalysisSettings());

		moveClass.execute(List.of(web, orders));

		assertThat(moveClass.getResults()).filteredOn(move -> move.className.equals("OrderService"))
			.singleElement()
			.satisfies(move -> {
				assertThat(move.targetModuleName).isEqualTo("shop/orders");
				assertThat(move.targetViolationsAfter).isEqualTo(move.targetViolationsBefore);
			});
	}

	private Module module(String name, Clazz... clazzes) {
		Module module = new Module(name, name, null, null, null, new ArrayList<>(List.of(clazzes)), 0.0, 0.0, 0);
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
