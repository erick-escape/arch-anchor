package com.archanchor.domains.clazz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.archanchor.domains.dependency.Dependency;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
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

}
