package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.services.parsers.LanguageParsers;
import com.archanchor.services.parsers.ScriptedLanguageParser;
import com.archanchor.services.rating.ActiveRatingWeights;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modules are made from what a parser reports, never from the sources themselves (#81):
 * one per directory that holds a class, whatever the language, the file extension or the
 * place the project keeps its sources in.
 */
class ModulesOfAnyLanguageTest {

	private static final Path PROJECT = Path.of("uploads", "shop");

	@Test
	void everyDirectoryHoldingAClassIsAModuleOrderedByPath() throws IOException {
		ScriptedLanguageParser php = new ScriptedLanguageParser("php")
			.declaring(PROJECT.resolve("app/Models/User.php"), "User", "App\\Models")
			.declaring(PROJECT.resolve("app/Http/Controllers/UserController.php"), "UserController",
					"App\\Http\\Controllers")
			.declaring(PROJECT.resolve("app/Models/Order.php"), "Order", "App\\Models");

		List<Module> modules = modulesReadBy(php);

		assertThat(modules).extracting(Module::getName).containsExactly("Http/Controllers", "app/Models");
		assertThat(modules.get(1).getClazzes()).extracting(Clazz::getName).containsExactly("Order", "User");
	}

	@Test
	void aClassKeepsWhatItsParserSaidOfIt() throws IOException {
		ScriptedLanguageParser php = new ScriptedLanguageParser("php").declaring(PROJECT.resolve("app/Models/User.php"),
				"User", "App\\Models");

		Clazz user = modulesReadBy(php).get(0).getClazzes().get(0);

		assertThat(user.getPackageName()).isEqualTo("App\\Models");
		assertThat(user.getFirstModule()).isEqualTo("Models");
		assertThat(user.getCurrentModule()).isEqualTo("Models");
		assertThat(user.getEnforceMode()).isEqualTo("ALLOW");
	}

	// A file's classes are not sorted by name: their order is the parser's, which is the
	// order they are declared in.
	@Test
	void classesDeclaredInOneFileKeepTheOrderTheirParserGaveThem() throws IOException {
		Path shapes = PROJECT.resolve("lib/shapes.php");
		ScriptedLanguageParser php = new ScriptedLanguageParser("php").declaring(shapes, "Square", null)
			.declaring(PROJECT.resolve("lib/areas.php"), "Area", null)
			.declaring(shapes, "Circle", null);

		List<Module> modules = modulesReadBy(php);

		assertThat(modules).singleElement()
			.satisfies(lib -> assertThat(lib.getClazzes()).extracting(Clazz::getName)
				.containsExactly("Area", "Square", "Circle"));
	}

	private static List<Module> modulesReadBy(ScriptedLanguageParser parser) throws IOException {
		ModuleService moduleService = new ModuleService(new LanguageParsers(List.of(parser)),
				new ActiveRatingWeights());
		return moduleService.getModules(PROJECT.toString());
	}

}
