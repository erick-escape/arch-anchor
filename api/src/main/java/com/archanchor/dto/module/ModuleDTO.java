package com.archanchor.dto.module;

import com.archanchor.dto.clazz.ClazzResponseDTO;
import com.archanchor.dto.dependencies.DependencyDTO;

import java.io.Serializable;

public record ModuleDTO(String id, String name, ClazzResponseDTO[] refClazzes, DependencyDTO[] refClazzesDependencies,
		DependencyDTO[] allDependencies, ClazzResponseDTO[] clazzes, Double similarity,
		Double avgRefClazzesSimilarity) implements Serializable {
}
