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
			.declaring(PROJECT.resolve("src/shop/Catalog.java"), "Catalog", "shop")
			.declaring(PROJECT.resolve("src/shop/Order.java"), "Order", "shop");
		ScriptedLanguageParser php = new ScriptedLanguageParser("php").declaring(PROJECT.resolve("docs/Example.php"),
				"Example", null);

		LanguageParsers parsers = new LanguageParsers(List.of(php, java));

		assertThat(parsers.parserOf(PROJECT)).containsSame(java);
		assertThat(parsers.clazzesOf(PROJECT)).extracting(ParsedClazz::name).containsExactly("Catalog", "Order");
	}

	@Test
	void languagesWithAsManySourcesAreSettledByName() throws IOException {
		ScriptedLanguageParser php = new ScriptedLanguageParser("php").declaring(PROJECT.resolve("app/User.php"),
				"User", "App");
		ScriptedLanguageParser java = new ScriptedLanguageParser("java")
			.declaring(PROJECT.resolve("src/shop/Catalog.java"), "Catalog", "shop");

		assertThat(new LanguageParsers(List.of(php, java)).parserOf(PROJECT)).containsSame(java);
		assertThat(new LanguageParsers(List.of(java, php)).parserOf(PROJECT)).containsSame(java);
	}

	@Test
	void aProjectNoParserFindsSourcesInHasNoParserAndNoClasses() throws IOException {
		LanguageParsers parsers = new LanguageParsers(
				List.of(new ScriptedLanguageParser("java"), new ScriptedLanguageParser("php")));

		assertThat(parsers.parserOf(PROJECT)).isEmpty();
		assertThat(parsers.clazzesOf(PROJECT)).isEmpty();
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
