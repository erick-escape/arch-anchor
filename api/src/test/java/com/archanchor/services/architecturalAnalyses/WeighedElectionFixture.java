package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A web module whose election the weights decide (#27). The two controllers are the most
 * alike, but either one as the reference class leaves two of the module's dependencies as
 * violations, whereas the report uses every dependency of the module and leaves none.
 * Equal weights elect the report; weights that favour similarity elect a controller. (The
 * shop of {@link AnalysedShopFixture} cannot show this: there every candidate leaves one
 * violation, so similarity decides whatever the weights.)
 *
 * <pre>
 * List&lt;Module&gt; modules = WeighedElectionFixture.analysedWeb(moduleService);
 * </pre>
 */
public final class WeighedElectionFixture {

	public static final String WEB = "shop/web";

	public static final String MOST_ALIKE = "CatalogController";

	public static final String OTHER_CONTROLLER = "OrderController";

	public static final String LEAVES_NO_VIOLATIONS = "SalesReport";

	private WeighedElectionFixture() {
	}

	/** The web module, analysed with the election weights {@code moduleService} has. */
	public static List<Module> analysedWeb(ModuleService moduleService) {
		List<Clazz> clazzes = List.of(clazzOf(MOST_ALIKE, "shop.service", "shop.model"),
				clazzOf(OTHER_CONTROLLER, "shop.service", "shop.model"),
				clazzOf(LEAVES_NO_VIOLATIONS, "shop.service", "shop.model", "java.sql", "java.time"));
		Module web = new Module(AnalysedShopFixture.idOf(WEB), WEB, null, null, null, new ArrayList<>(clazzes), 0.0,
				0.0, 0);
		AnalysedShopFixture.analyse(web, moduleService);
		return List.of(web);
	}

	private static Clazz clazzOf(String name, String... packageNames) {
		List<Dependency> dependencies = new ArrayList<>(Arrays.stream(packageNames).map(Dependency::new).toList());
		return new Clazz(AnalysedShopFixture.idOf(name), name, dependencies, 0.0, 0.0, WEB, WEB, "ALLOW");
	}

}
