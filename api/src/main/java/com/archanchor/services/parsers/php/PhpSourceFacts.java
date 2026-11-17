package com.archanchor.services.parsers.php;

import java.util.List;

/**
 * What one PHP file says about classes, in the project's own terms rather than a syntax
 * tree's (#85): the namespaces it writes code in and, for each, the classes it imports
 * and the classes it declares. It is all the PHP parser needs of a file, so the library
 * that reads PHP can be replaced without touching the parser.
 *
 * <pre>
 * PhpSourceFacts facts = reader.factsOf("&lt;?php namespace App\\Models; class Order extends Record {}");
 * facts.namespaceBlocks().get(0).declarations().get(0).name(); // "Order"
 * </pre>
 */
public record PhpSourceFacts(List<NamespaceBlock> namespaceBlocks) {

	/**
	 * The code a file writes in one namespace. The namespace is written without a leading
	 * backslash and is empty for the global one. A file usually has one block; it has
	 * none when it declares no class.
	 */
	public record NamespaceBlock(String namespace, List<ClassImport> classImports,
			List<ClassDeclaration> declarations) {
	}

	/**
	 * A class brought into a namespace block by {@code use}. The qualified name has no
	 * leading backslash; the alias is the name the block's code calls the class by, its
	 * last segment unless {@code as} gives another.
	 *
	 * <pre>
	 * new ClassImport("App\\Models\\Invoice", "Bill"); // use App\Models\Invoice as Bill;
	 * </pre>
	 */
	public record ClassImport(String qualifiedName, String alias) {
	}

	/**
	 * A class, interface, trait or enum, with every name it writes where PHP expects a
	 * class, as it is written and in the order of the source: {@code Order},
	 * {@code Models\Order}, {@code \App\Models\Order}, {@code namespace\Order}. A name
	 * written twice is there twice. What the names refer to is for
	 * {@link PhpNameResolver} to say.
	 */
	public record ClassDeclaration(String name, List<String> writtenClassNames) {
	}

}
