package com.archanchor.services;

import com.archanchor.dto.module.ModuleDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ModuleService} without the {@code modules.bin} file: the saved modules live in a
 * field. Tests can exercise everything that loads and saves modules without touching the
 * local state of whoever runs them.
 *
 * <pre>
 * InMemoryModuleService moduleService = new InMemoryModuleService();
 * moduleService.saveModules(List.of(webModule));
 * </pre>
 */
public class InMemoryModuleService extends ModuleService {

	private List<ModuleDTO> savedModules = new ArrayList<>();

	public InMemoryModuleService() {
		super(null);
	}

	@Override
	public void saveModules(List<ModuleDTO> modulesList) {
		savedModules = new ArrayList<>(modulesList);
	}

	@Override
	public List<ModuleDTO> getModulesFromFile() {
		return new ArrayList<>(savedModules);
	}

}
