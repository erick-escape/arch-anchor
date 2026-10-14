package com.archanchor.dto.module;

import com.archanchor.dto.clazz.ClazzResponseDTO;
import com.archanchor.dto.dependencies.DependencyDTO;

import java.io.Serializable;

/**
 * A module as the API returns it and {@code modules.bin} keeps it.
 * {@code refClazzesChosenByHand} says the architect picked the reference classes; a
 * {@code modules.bin} written before it existed reads it as false, since record
 * serialization does not compare serialVersionUIDs.
 */
public record ModuleDTO(String id, String name, ClazzResponseDTO[] refClazzes, DependencyDTO[] refClazzesDependencies,
		DependencyDTO[] allDependencies, ClazzResponseDTO[] clazzes, Double similarity, Double avgRefClazzesSimilarity,
		boolean refClazzesChosenByHand) implements Serializable {
}
