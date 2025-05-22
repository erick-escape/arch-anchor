package tcc.com.viewer.dto.clazz;

import tcc.com.viewer.dto.dependencies.DependencyDTO;

import java.io.Serializable;

public record ClazzResponseDTO(
        String id,
        String name,
        DependencyDTO[] dependencies,
        Double similarity,
        String firstModule,
        String currentModule) implements Serializable {
}