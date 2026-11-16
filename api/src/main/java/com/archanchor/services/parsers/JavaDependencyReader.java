package com.archanchor.services.parsers;

import com.archanchor.domains.dependency.Dependency;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads what one Java source file depends on (#81). It separates how a Java project is
 * laid out, which {@link JavaParser} knows, from how a file's types are resolved, which
 * needs the symbol solver and the project's libraries: the first can then be tested on
 * empty files, with a reader that resolves nothing.
 *
 * <pre>
 * List&lt;Dependency&gt; dependencies = reader.getDependencies(Path.of("src/main/java/shop/web/CatalogController.java"));
 * </pre>
 */
public interface JavaDependencyReader {

	/** The file's dependencies, none if it cannot be parsed. */
	List<Dependency> getDependencies(Path javaSource);

	/**
	 * Forgets what was resolved for the project read before, so that its types do not
	 * answer for the next one's.
	 */
	void clearCache();

}
