package tcc.com.viewer.services.architecturalAnalyses;

import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.services.ModuleService;

import java.util.List;

public abstract class ArchitecturalAnalysis {
    protected final ModuleService moduleService;

    public ArchitecturalAnalysis(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    public abstract void execute(List<Module> modules);
}
