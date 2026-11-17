package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassImport;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Says which class a name written in a namespace block refers to, by the rules PHP
 * applies when it compiles the file (#85). They need nothing but the block's namespace
 * and its imports: PHP never looks at what exists, and a class name, unlike a function
 * name, never falls back to the global namespace.
 *
 * <pre>
 * // namespace App\Http; use App\Models\User; use Doctrine\ORM\Mapping as ORM;
 * names.qualifiedNameOf("User"); // App\Models\User
 * names.qualifiedNameOf("Kernel"); // App\Http\Kernel
 * names.qualifiedNameOf("ORM\\Entity"); // Doctrine\ORM\Mapping\Entity
 * names.qualifiedNameOf("\\PDO"); // PDO
 * </pre>
 */
final class PhpNameResolver {

	private static final String CURRENT_NAMESPACE = "namespace";

	private final String namespace;

	private final List<ClassImport> classImports;

	// Aliases in lower case: PHP compares class names without regard to case.
	private final Map<String, String> importedNameByAlias = new HashMap<>();

	PhpNameResolver(NamespaceBlock block) {
		this.namespace = block.namespace();
		this.classImports = block.classImports();
		classImports
			.forEach(imported -> importedNameByAlias.put(lowerCase(imported.alias()), imported.qualifiedName()));
	}

	/** The qualified name, without a leading backslash, of a name as it is written. */
	String qualifiedNameOf(String writtenName) {
		if (writtenName.charAt(0) == PhpClassName.SEPARATOR) {
			return writtenName.substring(1);
		}
		String firstSegment = firstSegmentOf(writtenName);
		String afterFirstSegment = writtenName.substring(firstSegment.length());
		if (!afterFirstSegment.isEmpty() && firstSegment.equalsIgnoreCase(CURRENT_NAMESPACE)) {
			return PhpClassName.in(namespace, afterFirstSegment.substring(1));
		}
		String imported = importedNameByAlias.get(lowerCase(firstSegment));
		return imported != null ? imported + afterFirstSegment : PhpClassName.in(namespace, writtenName);
	}

	/**
	 * The imports that name a class: all of them, used or not, but those the block uses
	 * as the start of a longer name. {@code use Doctrine\ORM\Mapping as ORM;} followed by
	 * {@code ORM\Entity} imports a namespace, and it is {@code Entity} the block depends
	 * on.
	 */
	List<String> importedClasses(Collection<String> writtenNames) {
		Set<String> aliasesOfNamespaces = writtenNames.stream()
			.filter(PhpNameResolver::startsWithAnAlias)
			.map(written -> lowerCase(firstSegmentOf(written)))
			.collect(Collectors.toSet());
		return classImports.stream()
			.filter(imported -> !aliasesOfNamespaces.contains(lowerCase(imported.alias())))
			.map(ClassImport::qualifiedName)
			.toList();
	}

	// Models\Order may start with an alias; \Models\Order and Order cannot.
	private static boolean startsWithAnAlias(String writtenName) {
		return writtenName.indexOf(PhpClassName.SEPARATOR) > 0;
	}

	private static String firstSegmentOf(String writtenName) {
		int firstSeparator = writtenName.indexOf(PhpClassName.SEPARATOR);
		return firstSeparator < 0 ? writtenName : writtenName.substring(0, firstSeparator);
	}

	private static String lowerCase(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

}
