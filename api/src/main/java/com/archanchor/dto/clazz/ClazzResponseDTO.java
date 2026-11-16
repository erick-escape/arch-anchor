package com.archanchor.dto.clazz;

import com.archanchor.dto.dependencies.DependencyDTO;

import java.io.Serializable;
import java.util.List;

/**
 * A class as the API returns it and {@code modules.bin} keeps it. {@code qualifiedName}
 * is the name other classes refer to it by (#83); a {@code modules.bin} written before it
 * existed reads it as null, and {@code Clazz.getQualifiedName} then works it out.
 */
public record ClazzResponseDTO(String id, String name, List<DependencyDTO> dependencies, Double similarity,
		Double avgSimilarityWithRefClazzes, String firstModule, String currentModule, String enforceMode,
		String packageName, String qualifiedName) implements Serializable {
}
