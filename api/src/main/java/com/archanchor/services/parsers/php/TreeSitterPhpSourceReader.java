package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import org.springframework.stereotype.Component;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterPhp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reads PHP with tree-sitter and its PHP grammar (#85), and is the only class that
 * touches either. What it looks for is written as one query, {@code php/class-facts.scm}:
 * the library matches it natively and hands back only the nodes captured, each of which
 * this class reports to a {@link PhpSourceFactsBuilder} by its position.
 * <p>
 * The grammar is a native library, shipped for Linux (glibc), macOS and Windows. It is
 * loaded when the first PHP source is read, never when the application starts, so that a
 * platform without it still analyses Java.
 *
 * <pre>
 * PhpSourceFacts facts = new TreeSitterPhpSourceReader().factsOf("&lt;?php class Order extends Record {}");
 * </pre>
 */
@Component
public class TreeSitterPhpSourceReader implements PhpSourceReader {

	private static final String QUERY_RESOURCE = "/php/class-facts.scm";

	private static final String SUPPORTED_PLATFORMS = "Linux with glibc on x86_64 or aarch64, macOS on x86_64 or aarch64, Windows on x86_64";

	// The grammar gives these a node of their own in a static call, but not in a type or
	// after new, where they look like any class name. They are the class itself or its
	// parent, never another class.
	private static final Set<String> RELATIVE_SCOPES = Set.of("self", "static", "parent");

	private final Supplier<TSLanguage> phpGrammar;

	private volatile LoadedGrammar loadedGrammar;

	public TreeSitterPhpSourceReader() {
		this(TreeSitterPhp::new);
	}

	// For the test of a platform the grammar is not shipped for.
	TreeSitterPhpSourceReader(Supplier<TSLanguage> phpGrammar) {
		this.phpGrammar = phpGrammar;
	}

	@Override
	public PhpSourceFacts factsOf(String phpSource) {
		LoadedGrammar grammar = grammar();
		// The library counts positions in the bytes of the source as UTF-8.
		byte[] utf8Source = phpSource.getBytes(StandardCharsets.UTF_8);
		PhpSourceFactsBuilder facts = new PhpSourceFactsBuilder();
		// A parser and a tree for each source, released with it: neither is shared
		// between
		// the threads of two analyses.
		try (TSParser parser = new TSParser()) {
			parser.setLanguage(grammar.language());
			try (TSTree tree = parser.parseString(null, phpSource); TSQueryCursor matches = new TSQueryCursor()) {
				matches.exec(grammar.query(), tree.getRootNode());
				reportMatches(matches, grammar, utf8Source, facts);
			}
		}
		return facts.build();
	}

	private static void reportMatches(TSQueryCursor matches, LoadedGrammar grammar, byte[] utf8Source,
			PhpSourceFactsBuilder facts) {
		TSQueryMatch match = new TSQueryMatch();
		while (matches.nextMatch(match)) {
			Map<String, Captured> captured = new HashMap<>();
			for (TSQueryCapture capture : match.getCaptures()) {
				captured.put(grammar.captureNames()[capture.getIndex()], Captured.of(capture.getNode(), utf8Source));
			}
			report(captured, facts);
		}
	}

	// Each pattern of the query captures one kind of thing, told by the capture it has.
	private static void report(Map<String, Captured> captured, PhpSourceFactsBuilder facts) {
		if (captured.containsKey("namespace")) {
			reportNamespace(captured, facts);
		}
		else if (captured.containsKey("import.name")) {
			reportImport(captured, facts);
		}
		else if (captured.containsKey("declaration")) {
			Captured declaration = captured.get("declaration");
			facts.declaration(declaration.start(), declaration.end(), captured.get("declaration.name").text());
		}
		else if (captured.containsKey("reference")) {
			reportWrittenClassName(captured.get("reference"), facts);
		}
	}

	private static void reportNamespace(Map<String, Captured> captured, PhpSourceFactsBuilder facts) {
		String namespace = captured.containsKey("namespace.name") ? captured.get("namespace.name").text() : "";
		Captured body = captured.get("namespace.body");
		if (body == null) {
			facts.namespaceStatement(captured.get("namespace").start(), namespace);
			return;
		}
		facts.namespaceBlock(body.start(), body.end(), namespace);
	}

	private static void reportImport(Map<String, Captured> captured, PhpSourceFactsBuilder facts) {
		Captured name = captured.get("import.name");
		String qualifiedName = withoutLeadingBackslash(captured.containsKey("import.prefix")
				? captured.get("import.prefix").text() + "\\" + name.text() : name.text());
		String alias = captured.containsKey("import.alias") ? captured.get("import.alias").text()
				: qualifiedName.substring(qualifiedName.lastIndexOf('\\') + 1);
		facts.classImport(name.start(), new ClassImport(qualifiedName, alias));
	}

	private static void reportWrittenClassName(Captured written, PhpSourceFactsBuilder facts) {
		if (!RELATIVE_SCOPES.contains(written.text().toLowerCase(Locale.ROOT))) {
			facts.writtenClassName(written.start(), written.text());
		}
	}

	private static String withoutLeadingBackslash(String name) {
		return name.startsWith("\\") ? name.substring(1) : name;
	}

	private LoadedGrammar grammar() {
		LoadedGrammar loaded = loadedGrammar;
		if (loaded != null) {
			return loaded;
		}
		synchronized (this) {
			if (loadedGrammar == null) {
				loadedGrammar = load();
			}
			return loadedGrammar;
		}
	}

	private LoadedGrammar load() {
		TSLanguage language = nativeGrammar();
		TSQuery query = new TSQuery(language, queryText());
		String[] captureNames = new String[query.getCaptureCount()];
		for (int id = 0; id < captureNames.length; id++) {
			captureNames[id] = query.getCaptureNameForId(id);
		}
		return new LoadedGrammar(language, query, captureNames);
	}

	// LinkageError is what a missing or unloadable native library is reported as; the
	// binding wraps some failures to extract it in a RuntimeException. It unpacks the
	// library into the temporary directory and loads it from there, so a directory that
	// is read-only or mounted noexec fails the same way as a platform without a library.
	private TSLanguage nativeGrammar() {
		try {
			return phpGrammar.get();
		}
		catch (LinkageError | RuntimeException unavailable) {
			throw new IllegalStateException("Could not load the tree-sitter PHP grammar on "
					+ System.getProperty("os.name") + " " + System.getProperty("os.arch")
					+ ": PHP projects cannot be read here. Expected one of: " + SUPPORTED_PLATFORMS
					+ ", with a temporary directory (" + System.getProperty("java.io.tmpdir")
					+ ") that a library can be written to and loaded from", unavailable);
		}
	}

	private static String queryText() {
		try (InputStream query = TreeSitterPhpSourceReader.class.getResourceAsStream(QUERY_RESOURCE)) {
			if (query == null) {
				throw new IllegalStateException(
						"Expected the query " + QUERY_RESOURCE + " on the classpath, found none");
			}
			return new String(query.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException unreadable) {
			throw new IllegalStateException("Could not read the query " + QUERY_RESOURCE, unreadable);
		}
	}

	// Loaded once and shared: a grammar and a compiled query are not changed by using
	// them.
	private record LoadedGrammar(TSLanguage language, TSQuery query, String[] captureNames) {
	}

	// A captured node: where it is and what the source says there. PHP 8 writes a name as
	// one token, but earlier versions allowed spaces around its backslashes.
	private record Captured(int start, int end, String text) {

		static Captured of(TSNode node, byte[] utf8Source) {
			int start = node.getStartByte();
			int end = node.getEndByte();
			String text = new String(utf8Source, start, end - start, StandardCharsets.UTF_8);
			return new Captured(start, end, text.replaceAll("\\s+", ""));
		}

	}

}
