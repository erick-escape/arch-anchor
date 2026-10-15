package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.domains.module.Module;
import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Regression suite for issue #43. The parser drops same-package references, so a module
 * read from disk never depends on its own classes; a class moved or merged into a module
 * kept its references to them, and the module seemed to depend on itself. Here
 * {@code OrderService} was moved to {@code shop.web} and imports
 * {@code shop.orders.Order}; back in the orders module, that reference is internal.
 */
class ModuleInternalReferencesTest {

	private final ModuleService moduleService = new ModuleService(null, new ActiveRatingWeights());

	@Test
	void aReferenceToAClassOfTheSameModuleIsNotAModuleDependency() {
		Module orders = analysed("shop/orders", order(), orderRepository(), orderServiceDeclaredIn("shop.web"));

		assertThat(orders.getModuleDependencies()).extracting(Dependency::getPackageName)
			.containsExactlyInAnyOrder("shop.model", "java.sql");
	}

	@Test
	void aReferenceToAClassOfTheSameModuleIsNotAViolation() {
		Module orders = analysed("shop/orders", order(), orderRepository(), orderServiceDeclaredIn("shop.web"));

		List<String> allowed = orders.getRefClazzesDependencies().stream().map(Dependency::getPackageName).toList();
		assertThat(orders.getModuleDependencies()).extracting(Dependency::getPackageName)
			.filteredOn(packageName -> !allowed.contains(packageName))
			.doesNotContain("shop.orders");
	}

	@Test
	void aReferenceToAClassOfTheSameModuleDoesNotLowerSimilarity() {
		Clazz order = order();
		Clazz service = orderServiceDeclaredIn("shop.web");
		Module orders = analysed("shop/orders", order, service);

		assertThat(moduleService.calculateSimilarity(service, order, ModuleScope.of(orders))).isCloseTo(1.0,
				within(1e-9));
	}

	// The two halves of a split package reference each other; merged, they are one
	// module.
	@Test
	void mergedModulesDoNotDependOnEachOther() {
		Clazz left = clazz("Left", "shop.billing", "shop.billing2.Right", "java.util.List");
		Clazz right = clazz("Right", "shop.billing2", "shop.billing.Left", "java.util.List");

		Module merged = analysed("shop/billing", left, right);

		assertThat(merged.getModuleDependencies()).extracting(Dependency::getPackageName).containsExactly("java.util");
	}

	// Only the types that are classes of the module are internal; others of the same
	// package still count.
	@Test
	void aDependencyKeepsTheTypesThatAreNotClassesOfTheModule() {
		Clazz service = clazz("OrderService", "shop.web", "shop.orders.Order", "shop.orders.Invoice");

		List<Dependency> dependencies = ModuleScope.of(List.of(order(), service)).dependenciesOf(service);

		assertThat(dependencies).singleElement().satisfies(dependency -> {
			assertThat(dependency.getPackageName()).isEqualTo("shop.orders");
			assertThat(dependency.getTypes()).extracting(Type::getFullyQualifiedName)
				.containsExactly("shop.orders.Invoice");
		});
	}

	@Test
	void aNestedTypeOfAClassOfTheModuleIsInternalToo() {
		Clazz service = clazz("OrderService", "shop.web", "shop.orders.Order.Status");

		assertThat(ModuleScope.of(List.of(order(), service)).dependenciesOf(service)).isEmpty();
	}

	// A class whose package is not known (e.g. built by hand) is never internal.
	@Test
	void aClassOfUnknownPackageKeepsAllItsReferences() {
		Clazz unknown = clazz("Order", null, "shop.model.Book");
		Clazz service = clazz("OrderService", "shop.web", "shop.orders.Order");

		assertThat(ModuleScope.of(List.of(unknown, service)).dependenciesOf(service))
			.extracting(Dependency::getPackageName)
			.containsExactly("shop.orders");
	}

	private Module analysed(String name, Clazz... clazzes) {
		Module module = new Module(name, name, null, null, null, new ArrayList<>(List.of(clazzes)), 0.0, 0.0, 0);
		AnalysedShopFixture.analyse(module, moduleService);
		return module;
	}

	private static Clazz order() {
		return clazz("Order", "shop.orders", "shop.model.Book");
	}

	private static Clazz orderRepository() {
		return clazz("OrderRepository", "shop.orders", "shop.model.Book", "java.sql.Connection");
	}

	private static Clazz orderServiceDeclaredIn(String packageName) {
		return clazz("OrderService", packageName, "shop.orders.Order", "shop.model.Book");
	}

	private static Clazz clazz(String name, String packageName, String... usedTypes) {
		List<Dependency> dependencies = new ArrayList<>();
		for (String usedType : usedTypes) {
			String usedPackage = usedType.substring(0, firstCapital(usedType) - 1);
			Dependency dependency = dependencies.stream()
				.filter(d -> d.getPackageName().equals(usedPackage))
				.findFirst()
				.orElseGet(() -> {
					Dependency created = new Dependency(usedPackage);
					dependencies.add(created);
					return created;
				});
			dependency.addType(new Type(usedType));
		}
		Clazz clazz = new Clazz(name, name, dependencies, 0.0, 0.0, "shop", "shop", "ALLOW");
		clazz.setPackageName(packageName);
		return clazz;
	}

	// The package ends before the first capitalised segment, as for the parser's fallback
	private static int firstCapital(String qualifiedName) {
		int start = 0;
		for (String segment : qualifiedName.split("\\.")) {
			if (Character.isUpperCase(segment.charAt(0))) {
				return start;
			}
			start += segment.length() + 1;
		}
		return qualifiedName.lastIndexOf('.') + 1;
	}

}
