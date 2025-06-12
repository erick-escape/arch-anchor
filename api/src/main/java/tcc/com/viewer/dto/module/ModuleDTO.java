package tcc.com.viewer.dto.module;

import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.dependencies.DependencyDTO;

import java.io.Serializable;

public record ModuleDTO(
        String id,
        String name,
        ClazzResponseDTO[] refClazzes,
        ClazzResponseDTO[] clazzes,
        DependencyDTO[] dependencies,
        Double similarity) implements Serializable {
}
