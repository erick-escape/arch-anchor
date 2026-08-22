package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;

import java.util.List;

public abstract class ArchitecturalAnalysis {

	protected final ModuleService moduleService;

	public ArchitecturalAnalysis(ModuleService moduleService) {
		this.moduleService = moduleService;
	}

	public abstract void execute(List<Module> modules);

}
