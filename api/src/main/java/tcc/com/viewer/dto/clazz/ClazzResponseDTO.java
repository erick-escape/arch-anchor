package tcc.com.viewer.dto.clazz;

import tcc.com.viewer.dto.dependencies.DependencyDTO;

import java.io.Serializable;
import java.util.List;

public record ClazzResponseDTO(
        String id,
        String name,
        List<DependencyDTO> dependencies,
        Double similarity,
        String firstModule,
        String currentModule) implements Serializable {
}