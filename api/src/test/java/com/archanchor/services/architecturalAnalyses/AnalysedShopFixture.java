package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;

import java.util.ArrayList;
import java.util.List;

/**
 * A two-layer shop whose metrics come from the real {@link ModuleService}, computed with
 * the same steps as {@code ProjectService.analyzeProject}. {@code SalesReport} sits in
 * the web module but depends on {@code java.sql}, like the repositories.
 *
 * <pre>
 * List&lt;Module&gt; modules = AnalysedShopFixture.webAndPersistence(new InMemoryModuleService());
 * String salesReportId = AnalysedShopFixture.idOf(AnalysedShopFixture.MISPLACED_CLASS);
 * </pre>
 */
public final class AnalysedShopFixture {

	public static final String WEB = "shop/web";

	public static final String PERSISTENCE = "shop/persistence";

	public static final String MISPLACED_CLASS = "SalesReport";

	private AnalysedShopFixture() {
	}

	public static List<Module> webAndPersistence(ModuleService moduleService) {
		Module web = moduleOf(WEB, controller("CatalogController"), controller("CustomerController"),
				controller("OrderController"), usesDatabase(MISPLACED_CLASS, WEB));
		Module persistence = moduleOf(PERSISTENCE, usesDatabase("BookRepository", PERSISTENCE),
				usesDatabase("OrderRepository", PERSISTENCE));
		List<Module> modules = List.of(web, persistence);
		modules.forEach(module -> analyse(module, moduleService));
		return modules;
	}

	private static Clazz controller(String name) {
		return clazzOf(name, WEB, "shop.service.ShopService", "shop.model.Book");
	}

	private static Clazz usesDatabase(String name, String moduleName) {
		return clazzOf(name, moduleName, "java.sql.Connection", "shop.model.Book");
	}

	/**
	 * The id the fixture gives the class or module of that name. Ids are derived from
	 * names, so two fixtures are comparable field by field, but never equal to them, so a
	 * name passed where an id belongs is caught.
	 */
	public static String idOf(String name) {
		return "id-of-" + name;
	}

	private static Clazz clazzOf(String name, String moduleName, String... usedTypes) {
		List<Dependency> dependencies = new ArrayList<>();
		for (String usedType : usedTypes) {
			dependencies.add(dependencyOn(usedType));
		}
		return new Clazz(idOf(name), name, dependencies, 0.0, 0.0, moduleName, moduleName, "ALLOW");
	}

	// ArchitectureViolation only reports a class when the offending dependency names a
	// type.
	private static Dependency dependencyOn(String fullyQualifiedType) {
		String packageName = fullyQualifiedType.substring(0, fullyQualifiedType.lastIndexOf('.'));
		Dependency dependency = new Dependency(packageName);
		dependency.addType(new Type(fullyQualifiedType));
		return dependency;
	}

	private static Module moduleOf(String name, Clazz... clazzes) {
		return new Module(idOf(name), name, null, null, null, new ArrayList<>(List.of(clazzes)), 0.0, 0.0, 0);
	}

	private static void analyse(Module module, ModuleService moduleService) {
		moduleService.populateModuleDependencies(module);
		moduleService.calculateClassSimilaritiesAndSelectRefClasses(module);
		moduleService.calculateAvgSimilarityWithRefClazzes(module);
		moduleService.calculateModuleSimilarity(module);
		moduleService.populateRefClazzesDependencies(module);
		moduleService.calculateModuleViolations(module);
	}

}
