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

	// The package the class declares, or null if unknown (e.g. the default package). With
	// the name it identifies the class in other classes' dependencies, so that a
	// reference
	// to a class of the same module is internal after a move or merge (#43).
	private String packageName;

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

	/** {@code shop.web.CatalogController}, or null if the package is unknown. */
	public String qualifiedName() {
		return packageName == null ? null : packageName + "." + name;
	}

}
