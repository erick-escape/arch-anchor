package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Puts together a file's {@link PhpSourceFacts} from what was found in it, each thing at
 * its position in the source (#85). Whoever reads the syntax reports namespaces, imports,
 * declarations and written class names in any order; which namespace an import is in, and
 * which declaration a name was written in, follow from the positions alone. A position is
 * any number that grows along the source, such as a byte offset.
 *
 * <pre>
 * PhpSourceFactsBuilder builder = new PhpSourceFactsBuilder();
 * builder.namespaceStatement(6, "App\\Models");
 * builder.declaration(30, 60, "Order");
 * builder.writtenClassName(50, "Record");
 * PhpSourceFacts facts = builder.build(); // App\Models declares Order, which names Record
 * </pre>
 */
final class PhpSourceFactsBuilder {

	private static final int END_OF_SOURCE = Integer.MAX_VALUE;

	private static final String GLOBAL_NAMESPACE = "";

	private final List<Span> namespaceStatements = new ArrayList<>();

	private final List<Span> namespaceBlocks = new ArrayList<>();

	private final List<Positioned<ClassImport>> classImports = new ArrayList<>();

	private final List<Span> declarations = new ArrayList<>();

	private final List<Positioned<String>> writtenClassNames = new ArrayList<>();

	/** {@code namespace App\Models;}, which holds what follows it up to the next one. */
	void namespaceStatement(int start, String namespace) {
		namespaceStatements.add(new Span(start, END_OF_SOURCE, namespace));
	}

	/**
	 * {@code namespace App\Models { ... }}, which holds what its braces enclose; with an
	 * empty name, the global namespace.
	 */
	void namespaceBlock(int bodyStart, int bodyEnd, String namespace) {
		namespaceBlocks.add(new Span(bodyStart, bodyEnd, namespace));
	}

	void classImport(int position, ClassImport classImport) {
		classImports.add(new Positioned<>(position, classImport));
	}

	/** A class, interface, trait or enum, from where it starts to where its body ends. */
	void declaration(int start, int end, String name) {
		declarations.add(new Span(start, end, name));
	}

	/** A name written where PHP expects a class. */
	void writtenClassName(int position, String writtenName) {
		writtenClassNames.add(new Positioned<>(position, writtenName));
	}

	PhpSourceFacts build() {
		List<Span> namespaces = namespacesInSourceOrder();
		List<NamespaceBlock> blocks = new ArrayList<>();
		for (Span namespace : namespaces) {
			List<ClassDeclaration> declared = declarationsIn(namespace, namespaces);
			if (!declared.isEmpty()) {
				blocks.add(new NamespaceBlock(namespace.name(), importsIn(namespace, namespaces), declared));
			}
		}
		return new PhpSourceFacts(blocks);
	}

	// The global namespace comes first and spans the file: it holds whatever no other
	// namespace does. A statement ends where the next namespace starts.
	private List<Span> namespacesInSourceOrder() {
		List<Span> written = new ArrayList<>(namespaceBlocks);
		written.addAll(namespaceStatements);
		written.sort(Comparator.comparingInt(Span::start));
		List<Span> namespaces = new ArrayList<>();
		namespaces.add(new Span(Integer.MIN_VALUE, END_OF_SOURCE, GLOBAL_NAMESPACE));
		for (int index = 0; index < written.size(); index++) {
			Span namespace = written.get(index);
			int nextStart = index + 1 < written.size() ? written.get(index + 1).start() : END_OF_SOURCE;
			namespaces.add(new Span(namespace.start(), Math.min(namespace.end(), nextStart), namespace.name()));
		}
		return namespaces;
	}

	private List<ClassImport> importsIn(Span namespace, List<Span> namespaces) {
		return classImports.stream()
			.filter(found -> namespaceAt(found.position(), namespaces).equals(namespace))
			.sorted(Comparator.comparingInt(Positioned::position))
			.map(Positioned::value)
			.toList();
	}

	private List<ClassDeclaration> declarationsIn(Span namespace, List<Span> namespaces) {
		return declarations.stream()
			.filter(declaration -> namespaceAt(declaration.start(), namespaces).equals(namespace))
			.sorted(Comparator.comparingInt(Span::start))
			.map(declaration -> new ClassDeclaration(declaration.name(), namesWrittenIn(declaration)))
			.toList();
	}

	private List<String> namesWrittenIn(Span declaration) {
		return writtenClassNames.stream()
			.filter(written -> declaration.equals(innermostDeclarationAt(written.position())))
			.sorted(Comparator.comparingInt(Positioned::position))
			.map(Positioned::value)
			.toList();
	}

	// The last namespace to start that holds the position; the global one holds them all.
	private static Span namespaceAt(int position, List<Span> namespaces) {
		Span holding = null;
		for (Span namespace : namespaces) {
			if (namespace.holds(position)) {
				holding = namespace;
			}
		}
		return holding;
	}

	// A declaration inside another starts after it, so the last to start is the
	// innermost.
	private Span innermostDeclarationAt(int position) {
		return declarations.stream()
			.filter(declaration -> declaration.holds(position))
			.max(Comparator.comparingInt(Span::start))
			.orElse(null);
	}

	private record Span(int start, int end, String name) {

		boolean holds(int position) {
			return start <= position && position < end;
		}

	}

	private record Positioned<T>(int position, T value) {
	}

}
