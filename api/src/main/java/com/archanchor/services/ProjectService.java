package com.archanchor.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.archanchor.domains.module.Module;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.dependencies.DependencyDTO;
import com.archanchor.dto.projects.ArchitecturalConstraintDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.dto.projects.RefClassConstraintDTO;
import com.archanchor.mapstruct.ModuleMapper;
import com.archanchor.mapstruct.ModuleMapperImpl;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import com.archanchor.services.parsers.ParserFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectService {

	private final ModuleService moduleService;

	private final ParserFactory parserFactory;

	private final ArchitecturalAnalysesRunner analysesRunner;

	private final ModuleMapper moduleMapper = new ModuleMapperImpl();

	public ProjectService(ModuleService moduleService, ParserFactory parserFactory,
			ArchitecturalAnalysesRunner analysesRunner) {
		this.moduleService = moduleService;
		this.parserFactory = parserFactory;
		this.analysesRunner = analysesRunner;
	}

	public ProjectAnalysesDTO analyzeProject(String projectName, String directoryPath) throws IOException {
		// Clear parser cache for fresh analysis to prevent duplicate processing
		parserFactory.clearProcessingCache();

		List<Module> modules = moduleService.getModules(directoryPath);

		for (Module module : modules) {
			moduleService.populateModuleDependencies(module);
			moduleService.calculateClassSimilaritiesAndSelectRefClasses(module);
			moduleService.calculateAvgSimilarityWithRefClazzes(module);
			moduleService.calculateModuleSimilarity(module);
			moduleService.populateRefClazzesDependencies(module);
			moduleService.calculateModuleViolations(module);
		}

		analysesRunner.executeAll(modules);

		// Convert modules to ModuleDTO
		List<ModuleDTO> modulesList = modules.stream().map(moduleMapper::toDto).collect(Collectors.toList());
		moduleService.saveModules(modulesList);

		double projectSimilarity = modulesList.stream().mapToDouble(ModuleDTO::similarity).average().orElse(0.0);

		List<ArchitecturalConstraintDTO> architecturalConstraints = modulesList.stream().map(m -> {
			RefClassConstraintDTO[] refClassConstraints = Arrays.stream(m.refClazzes())
				.map(rc -> new RefClassConstraintDTO(rc.id(), rc.name(), rc.enforceMode(),
						rc.dependencies().toArray(new DependencyDTO[0])))
				.toArray(RefClassConstraintDTO[]::new);
			return new ArchitecturalConstraintDTO(m.id(), m.name(), refClassConstraints);
		}).collect(Collectors.toList());

		ProjectAnalysesDTO projectAnalysesDTO = new ProjectAnalysesDTO(UUID.randomUUID().toString(), projectName,
				modulesList, projectSimilarity, architecturalConstraints);
		moduleService.saveProjectAnalyses(projectAnalysesDTO);

		return projectAnalysesDTO;
	}

}
