package com.archanchor.services.parsers.php;

import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The classes a PHP project declares, by qualified name (#85). PHP compares class names
 * without regard to case, so {@code new order()} refers to {@code Order}; a reference is
 * traced to the module holding a class by the class's name exactly as declared, so every
 * reference to a class of the project is spelled the way its declaration spells it.
 *
 * <pre>
 * PhpProjectClasses declared = PhpProjectClasses.declaredIn(factsOfEverySource);
 * declared.declaredNameOf("app\\models\\order"); // Optional[App\Models\Order]
 * </pre>
 */
final class PhpProjectClasses {

	private final Map<String, String> declaredNameByLowerCaseName = new HashMap<>();

	private PhpProjectClasses() {
	}

	static PhpProjectClasses declaredIn(Collection<PhpSourceFacts> sources) {
		PhpProjectClasses declared = new PhpProjectClasses();
		for (PhpSourceFacts source : sources) {
			source.namespaceBlocks().forEach(declared::addDeclarationsOf);
		}
		return declared;
	}

	/** The name as its declaration spells it, if the project declares such a class. */
	Optional<String> declaredNameOf(String qualifiedName) {
		return Optional.ofNullable(declaredNameByLowerCaseName.get(qualifiedName.toLowerCase(Locale.ROOT)));
	}

	// When two files declare one name, the first read keeps it; sources are read in a
	// fixed order, so it is always the same one.
	private void addDeclarationsOf(NamespaceBlock block) {
		for (ClassDeclaration declaration : block.declarations()) {
			String qualifiedName = PhpClassName.in(block.namespace(), declaration.name());
			declaredNameByLowerCaseName.putIfAbsent(qualifiedName.toLowerCase(Locale.ROOT), qualifiedName);
		}
	}

}
