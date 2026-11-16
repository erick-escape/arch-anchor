package com.archanchor.services.parsers;

import com.archanchor.domains.dependency.Dependency;

import java.nio.file.Path;
import java.util.List;

/**
 * A class as a {@link LanguageParser} read it from the sources (#81). The directory of
 * its source file decides which module it starts in; the package is the one the class
 * declares, or null when it declares none.
 *
 * <pre>
 * new ParsedClazz(Path.of("uploads/bookstore/src/main/java/bookstore/web/SalesReport.java"), "SalesReport",
 * 		"bookstore.web", dependencies);
 * </pre>
 */
public record ParsedClazz(Path sourceFile, String name, String packageName, List<Dependency> dependencies) {
}
