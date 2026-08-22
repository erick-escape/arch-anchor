package com.archanchor.dto.clazz;

import com.archanchor.dto.dependencies.DependencyDTO;

import java.io.Serializable;
import java.util.List;

public record ClazzResponseDTO(String id, String name, List<DependencyDTO> dependencies, Double similarity,
		Double avgSimilarityWithRefClazzes, String firstModule, String currentModule,
		String enforceMode) implements Serializable {
}