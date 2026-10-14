package com.archanchor.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.archanchor.domains.module.Module;
import com.archanchor.domains.rating.RatingWeights;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.dependencies.DependencyDTO;
import com.archanchor.dto.projects.ArchitecturalConstraintDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.dto.projects.RefClassConstraintDTO;
import com.archanchor.mapstruct.ModuleMapper;
import com.archanchor.mapstruct.ModuleMapperImpl;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import com.archanchor.services.parsers.ParserFactory;
import com.archanchor.services.rating.ActiveRatingWeights;
import com.archanchor.services.rating.RatingWeightsStore;

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

	private final RatingWeightsStore ratingWeightsStore;

	private final ActiveRatingWeights activeWeights;

	private final ModuleMapper moduleMapper = new ModuleMapperImpl();

	public ProjectService(ModuleService moduleService, ParserFactory parserFactory,
			ArchitecturalAnalysesRunner analysesRunner, RatingWeightsStore ratingWeightsStore,
			ActiveRatingWeights activeWeights) {
		this.moduleService = moduleService;
		this.parserFactory = parserFactory;
		this.analysesRunner = analysesRunner;
		this.ratingWeightsStore = ratingWeightsStore;
		this.activeWeights = activeWeights;
	}

	/**
	 * Re-runs the analyses on the modules as saved, so that the recommendations describe
	 * the structure after a module operation instead of the project as first analysed.
	 * Without it, a recommendation that was just applied is offered again (#19).
	 *
	 * <pre>
	 * moduleController.moveClass(classId, sourceModuleId, targetModuleId);
	 * projectService.refreshRecommendations();
	 * </pre>
	 */
	public void refreshRecommendations() {
		analysesRunner.executeAll(moduleService.savedModulesForAnalysis());
	}

	public ProjectAnalysesDTO analyzeProject(String projectName, String directoryPath) throws IOException {
		// Clear parser cache for fresh analysis to prevent duplicate processing
		parserFactory.clearProcessingCache();
		activeWeights.adopt(projectName, ratingWeightsStore.weightsOf(projectName));

		List<Module> modules = moduleService.getModules(directoryPath);
		modules.forEach(this::electAndMeasure);
		analysesRunner.executeAll(modules);

		return saveAnalyses(projectName, modules);
	}

	public RatingWeights ratingWeightsOf(String projectName) {
		return ratingWeightsStore.weightsOf(projectName);
	}

	/**
	 * Saves the project's weights. If its modules are the saved ones, they are re-elected
	 * with the new weights, except those whose reference classes the architect chose, and
	 * the analyses run again; module operations are kept, since the sources are not read.
	 * Any other project picks the weights up when it is next analysed (#27).
	 *
	 * <pre>
	 * projectService.updateRatingWeights("bookstore", new RatingWeights(WeightPair.EQUAL, new WeightPair(0.9, 0.1)));
	 * </pre>
	 */
	public RatingWeights updateRatingWeights(String projectName, RatingWeights weights) {
		ratingWeightsStore.save(projectName, weights);
		if (activeWeights.appliesTo(projectName)) {
			activeWeights.adopt(projectName, weights);
			reElectSavedModules(projectName);
		}
		return weights;
	}

	/** A deleted project's weights must not pass to a new project of the same name. */
	public void forgetRatingWeights(String projectName) {
		ratingWeightsStore.forget(projectName);
	}

	// Re-electing even when only the recommendation pair changed costs little, and it
	// also brings modules elected under other weights, e.g. before a restart, in line.
	private void reElectSavedModules(String projectName) {
		List<Module> modules = moduleService.savedModulesForAnalysis();
		modules.stream().filter(module -> !module.isRefClazzesChosenByHand()).forEach(this::electAndMeasure);
		analysesRunner.executeAll(modules);
		saveAnalyses(projectName, modules);
	}

	private void electAndMeasure(Module module) {
		moduleService.populateModuleDependencies(module);
		moduleService.calculateClassSimilaritiesAndSelectRefClasses(module);
		moduleService.calculateAvgSimilarityWithRefClazzes(module);
		moduleService.calculateModuleSimilarity(module);
		moduleService.populateRefClazzesDependencies(module);
		moduleService.calculateModuleViolations(module);
	}

	private ProjectAnalysesDTO saveAnalyses(String projectName, List<Module> modules) {
		List<ModuleDTO> modulesList = modules.stream().map(moduleMapper::toDto).collect(Collectors.toList());
		moduleService.saveModules(modulesList);

		double projectSimilarity = modulesList.stream().mapToDouble(ModuleDTO::similarity).average().orElse(0.0);
		ProjectAnalysesDTO projectAnalysesDTO = new ProjectAnalysesDTO(UUID.randomUUID().toString(), projectName,
				modulesList, projectSimilarity, architecturalConstraintsOf(modulesList));
		moduleService.saveProjectAnalyses(projectAnalysesDTO);
		return projectAnalysesDTO;
	}

	private static List<ArchitecturalConstraintDTO> architecturalConstraintsOf(List<ModuleDTO> modulesList) {
		return modulesList.stream().map(m -> {
			RefClassConstraintDTO[] refClassConstraints = Arrays.stream(m.refClazzes())
				.map(rc -> new RefClassConstraintDTO(rc.id(), rc.name(), rc.enforceMode(),
						rc.dependencies().toArray(new DependencyDTO[0])))
				.toArray(RefClassConstraintDTO[]::new);
			return new ArchitecturalConstraintDTO(m.id(), m.name(), refClassConstraints);
		}).collect(Collectors.toList());
	}

}
