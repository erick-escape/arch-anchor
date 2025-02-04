package tcc.com.viewer.dto.clazz;

import tcc.com.viewer.dto.dependencies.DependencyDTO;

public record ClazzResponseDTO(String name, DependencyDTO[] dependencies, Double similarity) {
}
