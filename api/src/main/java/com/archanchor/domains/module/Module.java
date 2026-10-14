package com.archanchor.domains.module;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Module {

	private String id;

	private String name;

	private List<Clazz> refClazzes;

	private List<Dependency> refClazzesDependencies;

	private List<Dependency> moduleDependencies;

	private List<Clazz> clazzes;

	private Double similarity;

	private Double avgRefClazzesSimilarity;

	private Integer violations;

	// Set when the architect picks the reference classes and cleared by an election, so
	// that a change of election weights re-elects every module but these (#27).
	private boolean refClazzesChosenByHand;

	// Written out rather than generated, so that the new field, false for a module read
	// from sources, did not have to be passed by every caller.
	public Module(String id, String name, List<Clazz> refClazzes, List<Dependency> refClazzesDependencies,
			List<Dependency> moduleDependencies, List<Clazz> clazzes, Double similarity, Double avgRefClazzesSimilarity,
			Integer violations) {
		this.id = id;
		this.name = name;
		this.refClazzes = refClazzes;
		this.refClazzesDependencies = refClazzesDependencies;
		this.moduleDependencies = moduleDependencies;
		this.clazzes = clazzes;
		this.similarity = similarity;
		this.avgRefClazzesSimilarity = avgRefClazzesSimilarity;
		this.violations = violations;
	}

}