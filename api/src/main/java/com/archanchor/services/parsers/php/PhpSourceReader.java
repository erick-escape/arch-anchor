package com.archanchor.services.parsers.php;

/**
 * Reads what a PHP file says about classes (#85). It stands between the PHP parser and
 * the library that understands PHP's syntax, so that the parser is tested without that
 * library, and the library can be changed without changing the parser.
 *
 * <pre>
 * PhpSourceFacts facts = reader.factsOf(Files.readString(Path.of("app/Models/Order.php")));
 * </pre>
 */
public interface PhpSourceReader {

	/**
	 * The namespaces, class imports and class declarations of the source. A source with
	 * syntax errors yields what could be read of it, and one that is not PHP yields
	 * nothing; neither is an error.
	 */
	PhpSourceFacts factsOf(String phpSource);

}
