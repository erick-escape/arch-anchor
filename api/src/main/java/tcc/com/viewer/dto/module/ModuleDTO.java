package tcc.com.viewer.dto.module;

import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.dependencies.DependencyDTO;
import tcc.com.viewer.dto.dependencies.DependencyOriginDTO;
import tcc.com.viewer.dto.rules.AllowedRuleDTO;

import java.io.Serializable;

public record ModuleDTO(
        String id,
        String name,
        ClazzResponseDTO[] refClazzes,
        AllowedRuleDTO[] allowedRules,
        DependencyOriginDTO[] allDependenciesOrigin,
        DependencyDTO[] refClazzesDependencies,
        DependencyDTO[] allDependencies,
        ClazzResponseDTO[] clazzes,
        Double similarity) implements Serializable {
}
