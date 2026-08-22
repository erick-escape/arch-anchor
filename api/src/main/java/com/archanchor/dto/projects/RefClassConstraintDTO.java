package com.archanchor.dto.projects;

import com.archanchor.dto.dependencies.DependencyDTO;

import java.io.Serializable;

public record RefClassConstraintDTO(String refClassId, String refClassName, String enforceMode,
		DependencyDTO[] dependencies) implements Serializable {
}
