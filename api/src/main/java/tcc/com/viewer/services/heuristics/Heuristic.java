package tcc.com.viewer.services.heuristics;

import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.services.ModuleService;

import java.util.List;

public abstract class Heuristic {
    protected final ModuleService moduleService;

    public Heuristic(ModuleService moduleService) {
        this.moduleService = moduleService;
    }
    
    public abstract void execute(List<Module> modules);
}
