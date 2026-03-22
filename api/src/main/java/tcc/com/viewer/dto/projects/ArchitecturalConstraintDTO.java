package tcc.com.viewer.dto.projects;

import tcc.com.viewer.dto.dependencies.DependencyDTO;

import java.io.Serializable;

public record ArchitecturalConstraintDTO(
        String moduleId,
        String moduleName,
        DependencyDTO[] refClazzesDependencies
) implements Serializable {}
