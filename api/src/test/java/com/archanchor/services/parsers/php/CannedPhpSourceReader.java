package com.archanchor.services.parsers.php;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stand-in for the reader that understands PHP: it answers with the facts a test wrote
 * down for a source's exact text, and with none for any other. With it the PHP parser is
 * tested on files whose content is a label, without the native grammar.
 *
 * <pre>
 * CannedPhpSourceReader reader = new CannedPhpSourceReader().answering("order", factsOfOrder);
 * </pre>
 */
final class CannedPhpSourceReader implements PhpSourceReader {

	private final Map<String, PhpSourceFacts> factsBySource = new HashMap<>();

	private final List<String> sourcesRead = new ArrayList<>();

	CannedPhpSourceReader answering(String phpSource, PhpSourceFacts facts) {
		factsBySource.put(phpSource, facts);
		return this;
	}

	/** Every source it was asked about, in order. */
	List<String> sourcesRead() {
		return sourcesRead;
	}

	@Override
	public PhpSourceFacts factsOf(String phpSource) {
		sourcesRead.add(phpSource);
		return factsBySource.getOrDefault(phpSource, new PhpSourceFacts(List.of()));
	}

}
