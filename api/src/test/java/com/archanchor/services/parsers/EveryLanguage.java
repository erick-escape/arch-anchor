package com.archanchor.services.parsers;

import com.archanchor.services.JavaParserService;
import com.archanchor.services.parsers.php.ComposerAutoload;
import com.archanchor.services.parsers.php.PhpParser;
import com.archanchor.services.parsers.php.PhpPlatformClasses;
import com.archanchor.services.parsers.php.PhpSourceFiles;
import com.archanchor.services.parsers.php.TreeSitterPhpSourceReader;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * The parsers of every language the tool reads, wired by hand as the application wires
 * them, for tests that need the real rules of where a project's sources are and what they
 * declare.
 *
 * <pre>
 * AnalysisRootLocator locator = new AnalysisRootLocator(EveryLanguage.parsers());
 * </pre>
 */
public final class EveryLanguage {

	private EveryLanguage() {
	}

	// A Java project that declares no dependencies, like the bookstore, is resolved from
	// the JDK alone, so its parser downloads nothing; the PHP grammar is loaded when the
	// first PHP source is read, so a test of a Java project never needs it.
	public static LanguageParsers parsers() {
		PhpParser phpParser = new PhpParser(new PhpSourceFiles(new ComposerAutoload(new ObjectMapper())),
				new TreeSitterPhpSourceReader(), new PhpPlatformClasses());
		return new LanguageParsers(List.of(new JavaParser(new JavaParserService()), phpParser));
	}

}
