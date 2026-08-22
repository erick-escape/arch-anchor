package com.archanchor.domains.module;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
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

}