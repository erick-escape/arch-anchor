package com.archanchor.services.parsers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Which parser reads a project (#81): one language per project, the one most of its
 * sources are written in.
 */
class LanguageParsersTest {

	private static final Path PROJECT = Path.of("uploads", "shop");

	@Test
	void aProjectIsReadInTheLanguageMostOfItsSourcesAreWrittenIn() throws IOException {
		ScriptedLanguageParser java = new ScriptedLanguageParser("java")
			.declaring(PROJECT.resolve("src/shop/Catalog.java"), "Catalog", "shop", "shop.Catalog")
			.declaring(PROJECT.resolve("src/shop/Order.java"), "Order", "shop", "shop.Order");
		ScriptedLanguageParser php = new ScriptedLanguageParser("php").declaring(PROJECT.resolve("docs/Example.php"),
				"Example", null, "Example");

		LanguageParsers parsers = new LanguageParsers(List.of(php, java));

		assertThat(parsers.parserOf(PROJECT)).containsSame(java);
		assertThat(parsers.clazzesOf(PROJECT)).extracting(ParsedClazz::name).containsExactly("Catalog", "Order");
	}

	@Test
	void languagesWithAsManySourcesAreSettledByName() throws IOException {
		ScriptedLanguageParser php = new ScriptedLanguageParser("php").declaring(PROJECT.resolve("app/User.php"),
				"User", "App", "App\\User");
		ScriptedLanguageParser java = new ScriptedLanguageParser("java")
			.declaring(PROJECT.resolve("src/shop/Catalog.java"), "Catalog", "shop", "shop.Catalog");

		assertThat(new LanguageParsers(List.of(php, java)).parserOf(PROJECT)).containsSame(java);
		assertThat(new LanguageParsers(List.of(java, php)).parserOf(PROJECT)).containsSame(java);
	}

	// Not analysed as a project without modules: the user is told what was looked for
	// (#87).
	@Test
	void aProjectNoParserFindsSourcesInIsRefusedWithWhatEachLanguageExpects() throws IOException {
		LanguageParsers parsers = new LanguageParsers(
				List.of(new ScriptedLanguageParser("php"), new ScriptedLanguageParser("java")));

		assertThat(parsers.parserOf(PROJECT)).isEmpty();
		assertThatThrownBy(() -> parsers.clazzesOf(PROJECT)).isInstanceOf(ProjectWithoutClassesException.class)
			.hasMessage("Found no sources to analyse in 'shop'. Expected java sources where a test puts them; "
					+ "or php sources where a test puts them.");
	}

	// A folder of scripts, or of templates: read, and nothing in it to group into
	// modules.
	@Test
	void aProjectWhoseSourcesDeclareNoClassIsRefusedWithHowManyWereRead() {
		LanguageParsers parsers = new LanguageParsers(
				List.of(new ScriptedLanguageParser("php").listing(PROJECT.resolve("index.php"))
					.listing(PROJECT.resolve("includes/db.php"))));

		assertThatThrownBy(() -> parsers.clazzesOf(PROJECT)).isInstanceOf(ProjectWithoutClassesException.class)
			.hasMessage(
					"Expected a class in the 2 php source files of 'shop', found none: there is nothing to analyse.");
	}

	@Test
	void aSingleSourceWithoutAClassIsCountedAsOne() {
		LanguageParsers parsers = new LanguageParsers(
				List.of(new ScriptedLanguageParser("php").listing(PROJECT.resolve("index.php"))));

		assertThatThrownBy(() -> parsers.clazzesOf(PROJECT)).hasMessage(
				"Expected a class in the 1 php source file of 'shop', found none: there is nothing to analyse.");
	}

	@Test
	void twoParsersOfOneLanguageAreRefusedNamingBoth() {
		List<LanguageParser> twoOfJava = List.of(new ScriptedLanguageParser("java"),
				new ScriptedLanguageParser("java"));

		assertThatThrownBy(() -> new LanguageParsers(twoOfJava)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("one parser per language")
			.hasMessageContaining(ScriptedLanguageParser.class.getName())
			.hasMessageContaining("'java'");
	}

}
