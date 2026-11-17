package com.archanchor.services.parsers.php;

/**
 * The two halves of a PHP class name (#85): {@code App\Models\Order} is {@code Order} in
 * the namespace {@code App\Models}. Names here have no leading backslash, and the global
 * namespace is the empty string.
 *
 * <pre>
 * PhpClassName.namespaceOf("App\\Models\\Order"); // "App\\Models"
 * PhpClassName.in("App\\Models", "Order"); // "App\\Models\\Order"
 * </pre>
 */
final class PhpClassName {

	static final char SEPARATOR = '\\';

	private PhpClassName() {
	}

	/** The namespace of a qualified name; empty for a class of the global namespace. */
	static String namespaceOf(String qualifiedName) {
		int lastSeparator = qualifiedName.lastIndexOf(SEPARATOR);
		return lastSeparator < 0 ? "" : qualifiedName.substring(0, lastSeparator);
	}

	static String simpleNameOf(String qualifiedName) {
		return qualifiedName.substring(qualifiedName.lastIndexOf(SEPARATOR) + 1);
	}

	/** The qualified name of a name written in a namespace. */
	static String in(String namespace, String name) {
		return namespace.isEmpty() ? name : namespace + SEPARATOR + name;
	}

}
