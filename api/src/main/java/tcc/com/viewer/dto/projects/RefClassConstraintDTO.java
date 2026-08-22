package tcc.com.viewer.dto.projects;

import tcc.com.viewer.dto.dependencies.DependencyDTO;

import java.io.Serializable;

public record RefClassConstraintDTO(String refClassId, String refClassName, String enforceMode,
		DependencyDTO[] dependencies) implements Serializable {
}
