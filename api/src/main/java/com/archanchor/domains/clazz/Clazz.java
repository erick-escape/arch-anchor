package com.archanchor.domains.clazz;

import lombok.Getter;
import lombok.Setter;
import com.archanchor.domains.dependency.Dependency;

import java.util.List;

@Getter
@Setter
public class Clazz {

	private String id;

	private String name;

	private List<Dependency> dependencies;

	private Double similarity;

	private Double avgSimilarityWithRefClazzes;

	private final String firstModule;

	private String currentModule;

	// "ALLOW" (default) or "MUST" — only meaningful when this class is a ref class
	private String enforceMode;

	// The package the class declares, or null if unknown (e.g. the default package).
	private String packageName;

	// The name other classes refer to the class by, as its parser read it (#83). It
	// identifies the class in other classes' dependencies, so that a reference to a class
	// of the same module is internal after a move or merge (#43).
	private String qualifiedName;

	public Clazz(String id, String name, List<Dependency> dependencies, Double similarity,
			Double avgSimilarityWithRefClazzes, String firstModule, String currentModule, String enforceMode) {
		this.id = id;
		this.name = name;
		this.dependencies = dependencies;
		this.similarity = similarity;
		this.avgSimilarityWithRefClazzes = avgSimilarityWithRefClazzes;
		this.firstModule = firstModule;
		this.currentModule = currentModule;
		this.enforceMode = enforceMode;
	}

	/**
	 * The name other classes refer to this class by, written as its language writes it:
	 * {@code shop.web.CatalogController} in Java, {@code App\Models\User} in PHP. Null if
	 * nothing can refer to it, as with a Java class of the default package.
	 * <p>
	 * A class saved before names were stored (#83) has none. Only Java was read then, so
	 * it is its package and its name joined by a dot.
	 *
	 * <pre>
	 * clazz.getQualifiedName(); // "shop.web.CatalogController"
	 * </pre>
	 */
	public String getQualifiedName() {
		if (qualifiedName != null || packageName == null) {
			return qualifiedName;
		}
		return packageName + "." + name;
	}

}
