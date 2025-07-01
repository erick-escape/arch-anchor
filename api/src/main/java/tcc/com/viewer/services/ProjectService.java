package tcc.com.viewer.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.mapstruct.ModuleMapper;
import tcc.com.viewer.mapstruct.ModuleMapperImpl;
import tcc.com.viewer.services.parsers.ParserFactory;
import tcc.com.viewer.services.heuristics.HeuristicRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectService {
    private final ModuleService moduleService;
    private final ParserFactory parserFactory;
    private final HeuristicRunner heuristicRunner;
    private final ModuleMapper moduleMapper = new ModuleMapperImpl();

    public ProjectService(ModuleService moduleService, ParserFactory parserFactory, HeuristicRunner heuristicRunner) {
        this.moduleService = moduleService;
        this.parserFactory = parserFactory;
        this.heuristicRunner = heuristicRunner;
    }

    public List<ModuleDTO> analyzeProject(String directoryPath) throws IOException {
        // Clear parser cache for fresh analysis to prevent duplicate processing
        parserFactory.clearProcessingCache();

        List<Module> modules = moduleService.getModules(directoryPath);

        for (Module module : modules) {
            moduleService.calculateClassSimilarities(module);
            moduleService.calculateModuleSimilarity(module);
            moduleService.populateAllowedRules(module);
        }

        heuristicRunner.executeAll(modules);

        // Convert modules to ModuleDTO
        List<ModuleDTO> modulesList = modules.stream()
                .map(moduleMapper::toDto)
                .collect(Collectors.toList());
        moduleService.saveModules(modulesList);

        return modulesList;
    }
}
