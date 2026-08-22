package com.archanchor.dto.projects;

import com.archanchor.dto.module.ModuleDTO;

import java.io.Serializable;
import java.util.List;

public record ProjectAnalysesDTO(String id, String projectName, List<ModuleDTO> modulesList, Double projectSimilarity,
		List<ArchitecturalConstraintDTO> architecturalConstraints) implements Serializable {
}
