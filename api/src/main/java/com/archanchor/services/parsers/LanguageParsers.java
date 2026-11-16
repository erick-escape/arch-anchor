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
 * two languages have as many, the first by name reads it.
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
		LanguageParser chosen = null;
		int mostSources = 0;
		for (LanguageParser parser : parsersByLanguage) {
			int sources = parser.sourceFilesOf(projectDirectory).size();
			if (sources > mostSources) {
				chosen = parser;
				mostSources = sources;
			}
		}
		return Optional.ofNullable(chosen);
	}

	/** The project's classes; none when no parser finds sources in it. */
	public List<ParsedClazz> clazzesOf(Path projectDirectory) throws IOException {
		Optional<LanguageParser> parser = parserOf(projectDirectory);
		if (parser.isEmpty()) {
			log.info("No sources in a language the tool reads under {}", projectDirectory);
			return List.of();
		}
		log.info("Reading {} as a {} project", projectDirectory, parser.get().language());
		return parser.get().clazzesOf(projectDirectory);
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
