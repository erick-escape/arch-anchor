package tcc.com.viewer.dto.module;

import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.dependencies.DependencyDTO;

public record ModuleDTO(String name, String refClass, ClazzResponseDTO[] clazzes, DependencyDTO[] dependencies, Double similarity) {
}
