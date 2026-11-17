package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which class a written name refers to (#85), by PHP's own rules for resolving class
 * names when it compiles a file.
 */
class PhpNameResolverTest {

	private final PhpNameResolver names = new PhpNameResolver(new NamespaceBlock(
			"App\\Http", List.of(new ClassImport("App\\Models\\User", "User"),
					new ClassImport("App\\Models\\Invoice", "Bill"), new ClassImport("Doctrine\\ORM\\Mapping", "ORM")),
			List.of()));

	@Test
	void aNameWithALeadingBackslashIsAlreadyQualified() {
		assertThat(names.qualifiedNameOf("\\PDO")).isEqualTo("PDO");
		assertThat(names.qualifiedNameOf("\\App\\Models\\Order")).isEqualTo("App\\Models\\Order");
	}

	@Test
	void anImportedNameIsTheClassItsImportNames() {
		assertThat(names.qualifiedNameOf("User")).isEqualTo("App\\Models\\User");
		assertThat(names.qualifiedNameOf("Bill")).isEqualTo("App\\Models\\Invoice");
	}

	@Test
	void aNameThatIsNotImportedIsInTheNamespaceItIsWrittenIn() {
		assertThat(names.qualifiedNameOf("Kernel")).isEqualTo("App\\Http\\Kernel");
		assertThat(names.qualifiedNameOf("Middleware\\Auth")).isEqualTo("App\\Http\\Middleware\\Auth");
	}

	// A class name never falls back to the global namespace, as a function name does.
	@Test
	void evenTheNameOfABuiltInClassIsInTheNamespaceItIsWrittenIn() {
		assertThat(names.qualifiedNameOf("Exception")).isEqualTo("App\\Http\\Exception");
	}

	@Test
	void aNameThatStartsWithAnAliasContinuesFromWhatTheAliasImports() {
		assertThat(names.qualifiedNameOf("ORM\\Entity")).isEqualTo("Doctrine\\ORM\\Mapping\\Entity");
		assertThat(names.qualifiedNameOf("ORM\\Id\\Generated")).isEqualTo("Doctrine\\ORM\\Mapping\\Id\\Generated");
	}

	@Test
	void theNamespaceKeywordStandsForTheNamespaceTheNameIsWrittenIn() {
		assertThat(names.qualifiedNameOf("namespace\\Kernel")).isEqualTo("App\\Http\\Kernel");
		assertThat(names.qualifiedNameOf("NAMESPACE\\Middleware\\Auth")).isEqualTo("App\\Http\\Middleware\\Auth");
	}

	@Test
	void aliasesAreComparedWithoutRegardToCase() {
		assertThat(names.qualifiedNameOf("user")).isEqualTo("App\\Models\\User");
		assertThat(names.qualifiedNameOf("orm\\Entity")).isEqualTo("Doctrine\\ORM\\Mapping\\Entity");
	}

	@Test
	void inTheGlobalNamespaceANameIsItself() {
		PhpNameResolver global = new PhpNameResolver(
				new NamespaceBlock("", List.of(new ClassImport("Vendor\\Mailer", "Mailer")), List.of()));

		assertThat(global.qualifiedNameOf("Swift_Mailer")).isEqualTo("Swift_Mailer");
		assertThat(global.qualifiedNameOf("Mailer")).isEqualTo("Vendor\\Mailer");
		assertThat(global.qualifiedNameOf("namespace\\Legacy")).isEqualTo("Legacy");
	}

	@Test
	void everyImportNamesAClassWhetherTheCodeUsesItOrNot() {
		assertThat(names.importedClasses(List.of("User"))).containsExactly("App\\Models\\User", "App\\Models\\Invoice",
				"Doctrine\\ORM\\Mapping");
	}

	// use Doctrine\ORM\Mapping as ORM; ... #[ORM\Entity]: Mapping is a namespace.
	@Test
	void anImportUsedAsTheStartOfALongerNameIsANamespaceNotAClass() {
		assertThat(names.importedClasses(List.of("User", "orm\\Entity", "\\ORM\\Unrelated")))
			.containsExactly("App\\Models\\User", "App\\Models\\Invoice");
	}

}
