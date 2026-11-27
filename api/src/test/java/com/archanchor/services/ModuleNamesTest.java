package com.archanchor.services;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * The names modules are shown and exported under (#89): a directory and its parent, as
 * always, unless that would give two modules one name.
 */
class ModuleNamesTest {

	private static final Path SHOP = Path.of("uploads", "shop");

	@Test
	void aModuleIsNamedAfterItsDirectoryAndThatDirectorysParent() {
		Path model = SHOP.resolve("src/main/java/bookstore/model");
		Path web = SHOP.resolve("src/main/java/bookstore/web");

		assertThat(ModuleNames.of(SHOP, List.of(model, web))).containsExactly(entry(model, "bookstore/model"),
				entry(web, "bookstore/web"));
	}

	@Test
	void modulesThatWouldShareANameTakeAsManyMoreDirectoriesAsTheyNeedToDiffer() {
		Path requests = SHOP.resolve("app/Http/Requests/Admin/Sale");
		Path services = SHOP.resolve("app/Services/Admin/Sale");
		Path models = SHOP.resolve("app/Models/Sale");

		Map<Path, String> names = ModuleNames.of(SHOP, List.of(requests, services, models));

		assertThat(names).containsEntry(requests, "Requests/Admin/Sale")
			.containsEntry(services, "Services/Admin/Sale")
			.containsEntry(models, "Models/Sale");
	}

	@Test
	void namesThatStillCollideKeepGrowingUntilTheyDiffer() {
		Path admin = SHOP.resolve("app/Http/Controllers/Admin/Sale");
		Path api = SHOP.resolve("api/Http/Controllers/Admin/Sale");

		assertThat(ModuleNames.of(SHOP, List.of(admin, api))).containsEntry(admin, "app/Http/Controllers/Admin/Sale")
			.containsEntry(api, "api/Http/Controllers/Admin/Sale");
	}

	// A module whose whole path is the end of another's: it can grow no further, so the
	// other one does, up to the project's name.
	@Test
	void aNameThatCannotGrowLeavesTheGrowingToTheOther() {
		Path shallow = SHOP.resolve("Admin/Sale");
		Path deep = SHOP.resolve("shop/Admin/Sale");

		assertThat(ModuleNames.of(SHOP, List.of(shallow, deep))).containsEntry(shallow, "shop/Admin/Sale")
			.containsEntry(deep, "shop/shop/Admin/Sale");
	}

	// Not after the directory the projects are kept in, which is the server's business.
	@Test
	void aModuleAtTheProjectRootIsNamedAfterTheProject() {
		Path app = SHOP.resolve("app");

		assertThat(ModuleNames.of(SHOP, List.of(SHOP, app))).containsEntry(SHOP, "shop").containsEntry(app, "shop/app");
	}

	@Test
	void theProjectIsNamedAfterItsDirectoryHoweverItsPathIsWritten() {
		Path project = Path.of("..", "examples", "bookstore");
		Path model = project.resolve("src/main/java/bookstore/model");

		assertThat(ModuleNames.of(project, List.of(project, model))).containsEntry(project, "bookstore")
			.containsEntry(model, "bookstore/model");
	}

	// A parser may list a source under a path written otherwise than the project's.
	@Test
	void aModuleIsNamedTheSameHoweverItsPathIsWritten() {
		Path project = Path.of(".", "uploads", "shop");
		Path app = SHOP.toAbsolutePath().resolve("app");

		assertThat(ModuleNames.of(project, List.of(SHOP, app))).containsEntry(SHOP, "shop")
			.containsEntry(app, "shop/app");
	}

}
