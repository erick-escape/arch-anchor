package com.archanchor.services.parsers.php;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.services.parsers.php.PhpSourceFacts.ClassDeclaration;
import com.archanchor.services.parsers.php.PhpSourceFacts.NamespaceBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a PHP class depends on (#85): the classes its file imports and the classes it
 * names, grouped by package in the order they first appear. A class's package is its
 * namespace; a class PHP itself provides is filed under {@code php} or {@code ext-<name>}
 * ({@link PhpPlatformClasses}); a class of no namespace that is neither PHP's nor the
 * project's is filed under {@value #GLOBAL_PACKAGE}.
 * <p>
 * As for Java, a reference to a class of the same package is not a dependency, and every
 * import counts, used or not: an import is the file saying what it depends on, and it is
 * also how a class named only in a PHPDoc comment is seen.
 *
 * <pre>
 * List&lt;Dependency&gt; dependencies = new PhpDependencies(projectClasses, platformClasses).of(block, declaration);
 * </pre>
 */
final class PhpDependencies {

	static final String GLOBAL_PACKAGE = "(global)";

	private final PhpProjectClasses projectClasses;

	private final PhpPlatformClasses platformClasses;

	PhpDependencies(PhpProjectClasses projectClasses, PhpPlatformClasses platformClasses) {
		this.projectClasses = projectClasses;
		this.platformClasses = platformClasses;
	}

	/**
	 * The dependencies of a class declared in a namespace block; a fresh list each time.
	 */
	List<Dependency> of(NamespaceBlock block, ClassDeclaration declaration) {
		Map<String, Dependency> dependencyByPackage = new LinkedHashMap<>();
		Set<String> alreadyListed = new HashSet<>();
		// A class does not depend on itself.
		alreadyListed.add(lowerCase(PhpClassName.in(block.namespace(), declaration.name())));
		for (String qualifiedName : classesReferredToBy(declaration, new PhpNameResolver(block))) {
			PackagedClass referred = packaged(qualifiedName);
			if (alreadyListed.add(lowerCase(referred.qualifiedName())) && !isInTheNamespaceOf(block, referred)) {
				dependencyByPackage.computeIfAbsent(referred.packageName(), Dependency::new).addType(referred.type());
			}
		}
		return new ArrayList<>(dependencyByPackage.values());
	}

	private static List<String> classesReferredToBy(ClassDeclaration declaration, PhpNameResolver names) {
		List<String> qualifiedNames = new ArrayList<>(names.importedClasses(declaration.writtenClassNames()));
		declaration.writtenClassNames().forEach(written -> qualifiedNames.add(names.qualifiedNameOf(written)));
		return qualifiedNames;
	}

	// A class the project declares wins over a built-in of the same name: PHP would not
	// load both, and the project's is the one its code refers to.
	private PackagedClass packaged(String qualifiedName) {
		Optional<String> declaredName = projectClasses.declaredNameOf(qualifiedName);
		if (declaredName.isPresent()) {
			return inItsNamespace(declaredName.get());
		}
		return platformClasses.find(qualifiedName)
			.map(builtIn -> new PackagedClass(builtIn.qualifiedName(), builtIn.packageName()))
			.orElseGet(() -> inItsNamespace(qualifiedName));
	}

	private static PackagedClass inItsNamespace(String qualifiedName) {
		String namespace = PhpClassName.namespaceOf(qualifiedName);
		return new PackagedClass(qualifiedName, namespace.isEmpty() ? GLOBAL_PACKAGE : namespace);
	}

	// Classes of the global namespace are not one package: they share no directory, so a
	// reference from one to another is kept.
	private static boolean isInTheNamespaceOf(NamespaceBlock block, PackagedClass referred) {
		return !block.namespace().isEmpty() && referred.packageName().equalsIgnoreCase(block.namespace());
	}

	private static String lowerCase(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	private record PackagedClass(String qualifiedName, String packageName) {

		Type type() {
			return new Type(qualifiedName, PhpClassName.simpleNameOf(qualifiedName));
		}

	}

}
