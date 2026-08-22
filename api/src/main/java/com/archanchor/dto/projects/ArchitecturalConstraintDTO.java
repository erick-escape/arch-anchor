package com.archanchor.dto.projects;

import java.io.Serializable;

public record ArchitecturalConstraintDTO(String moduleId, String moduleName,
		RefClassConstraintDTO[] refClassConstraints) implements Serializable {
}
