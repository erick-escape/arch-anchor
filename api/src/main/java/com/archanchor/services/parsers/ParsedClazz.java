package com.archanchor.services.parsers;

import com.archanchor.domains.dependency.Dependency;

import java.nio.file.Path;
import java.util.List;

/**
 * A class as a {@link LanguageParser} read it from the sources (#81). The directory of
 * its source file decides which module it starts in; the package is the one the class
 * declares, or null when it declares none.
 * <p>
 * The qualified name is what other classes refer to the class by, written as the language
 * writes it: {@code bookstore.web.SalesReport} in Java, {@code App\Models\User} in PHP
 * (#83). It must be the name the parser gives the class in the dependencies of the
 * classes that use it ({@code Type.fullyQualifiedName}), since that is how a dependency
 * is traced to the module holding the class. Null for a class nothing can refer to, such
 * as a Java class of the default package.
 *
 * <pre>
 * new ParsedClazz(Path.of("uploads/bookstore/src/main/java/bookstore/web/SalesReport.java"), "SalesReport",
 * 		"bookstore.web", "bookstore.web.SalesReport", dependencies);
 * </pre>
 */
public record ParsedClazz(Path sourceFile, String name, String packageName, String qualifiedName,
		List<Dependency> dependencies) {
}
