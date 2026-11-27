package com.archanchor.services.parsers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Every {@link LanguageParser} the tool has, and which of them reads a given project
 * (#81). A project is read in one language, the one most of its production sources are
 * written in, so that a stray file in another language does not add modules to it; when
 * two languages have as many, the first by name reads it. A project nothing can be read
 * from is refused with what each language expects (#87).
 *
 * <pre>
 * List&lt;ParsedClazz&gt; clazzes = languageParsers.clazzesOf(Path.of("uploads/bookstore"));
 * </pre>
 */
@Slf4j
@Component
public class LanguageParsers {

	private final List<LanguageParser> parsersByLanguage;

	public LanguageParsers(List<LanguageParser> parsers) {
		this.parsersByLanguage = parsers.stream().sorted(Comparator.comparing(LanguageParser::language)).toList();
		rejectASharedLanguage();
	}

	/** The parser of the language the project is written in, if any parser reads it. */
	public Optional<LanguageParser> parserOf(Path projectDirectory) throws IOException {
		return mostSourcesIn(projectDirectory).map(ChosenParser::parser);
	}

	/**
	 * The project's classes, read in its language.
	 * @throws ProjectWithoutClassesException when no parser finds sources in it, or its
	 * sources declare no class
	 */
	public List<ParsedClazz> clazzesOf(Path projectDirectory) throws IOException {
		ChosenParser chosen = mostSourcesIn(projectDirectory).orElseThrow(
				() -> ProjectWithoutClassesException.withoutSources(projectDirectory, expectedSourcesOfEachLanguage()));
		LanguageParser parser = chosen.parser();
		log.info("Reading {} as a {} project", projectDirectory, parser.language());
		List<ParsedClazz> clazzes = parser.clazzesOf(projectDirectory);
		if (clazzes.isEmpty()) {
			throw ProjectWithoutClassesException.withoutClasses(projectDirectory, parser.language(),
					chosen.sourceFiles());
		}
		return clazzes;
	}

	/**
	 * What each language looks for, in the order of their names, for telling the user.
	 */
	public List<String> expectedSourcesOfEachLanguage() {
		return parsersByLanguage.stream().map(LanguageParser::expectedSources).toList();
	}

	private Optional<ChosenParser> mostSourcesIn(Path projectDirectory) throws IOException {
		ChosenParser chosen = null;
		for (LanguageParser parser : parsersByLanguage) {
			int sources = parser.sourceFilesOf(projectDirectory).size();
			if (sources > 0 && (chosen == null || sources > chosen.sourceFiles())) {
				chosen = new ChosenParser(parser, sources);
			}
		}
		return Optional.ofNullable(chosen);
	}

	private record ChosenParser(LanguageParser parser, int sourceFiles) {
	}

	// Sorted by language, so two parsers of the same one are next to each other.
	private void rejectASharedLanguage() {
		for (int next = 1; next < parsersByLanguage.size(); next++) {
			LanguageParser parser = parsersByLanguage.get(next);
			LanguageParser previous = parsersByLanguage.get(next - 1);
			if (parser.language().equals(previous.language())) {
				throw new IllegalArgumentException(
						"Expected one parser per language, got " + previous.getClass().getName() + " and "
								+ parser.getClass().getName() + " for '" + parser.language() + "'");
			}
		}
	}

}
