package com.archanchor.services;

import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.services.parsers.ParserFactory;
import com.archanchor.services.rating.ActiveRatingWeights;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ModuleService} without the {@code modules.bin} and {@code project-analyses.bin}
 * files: what would be saved lives in fields. Tests can exercise everything that loads
 * and saves modules without touching the local state of whoever runs them.
 *
 * <pre>
 * InMemoryModuleService moduleService = new InMemoryModuleService();
 * moduleService.saveModules(List.of(webModule));
 * </pre>
 */
public class InMemoryModuleService extends ModuleService {

	private List<ModuleDTO> savedModules = new ArrayList<>();

	private ProjectAnalysesDTO savedProjectAnalyses;

	public InMemoryModuleService() {
		this(null, new ActiveRatingWeights());
	}

	/**
	 * For tests that read sources from disk, such as the bookstore example, or that set
	 * the weights of the election.
	 */
	public InMemoryModuleService(ParserFactory parserFactory, ActiveRatingWeights activeWeights) {
		super(parserFactory, activeWeights);
	}

	@Override
	public void saveModules(List<ModuleDTO> modulesList) {
		savedModules = new ArrayList<>(modulesList);
	}

	@Override
	public List<ModuleDTO> getModulesFromFile() {
		return new ArrayList<>(savedModules);
	}

	@Override
	public void saveProjectAnalyses(ProjectAnalysesDTO projectAnalyses) {
		savedProjectAnalyses = projectAnalyses;
	}

	@Override
	public ProjectAnalysesDTO getProjectAnalysesFromFile() {
		return savedProjectAnalyses;
	}

}
