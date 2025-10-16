package tcc.com.viewer.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.mapstruct.ModuleMapper;
import tcc.com.viewer.mapstruct.ModuleMapperImpl;
import tcc.com.viewer.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import tcc.com.viewer.services.parsers.ParserFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectService {
    private final ModuleService moduleService;
    private final ParserFactory parserFactory;
    private final ArchitecturalAnalysesRunner analysesRunner;
    private final ModuleMapper moduleMapper = new ModuleMapperImpl();

    public ProjectService(ModuleService moduleService, ParserFactory parserFactory, ArchitecturalAnalysesRunner analysesRunner) {
        this.moduleService = moduleService;
        this.parserFactory = parserFactory;
        this.analysesRunner = analysesRunner;
    }

    public List<ModuleDTO> analyzeProject(String directoryPath) throws IOException {
        // Clear parser cache for fresh analysis to prevent duplicate processing
        parserFactory.clearProcessingCache();

        List<Module> modules = moduleService.getModules(directoryPath);

        for (Module module : modules) {
            moduleService.populateModuleDependencies(module);
            moduleService.calculateClassSimilarities(module);
            moduleService.calculateAvgSimilarityWithRefClazzes(module);
            moduleService.calculateModuleSimilarity(module);
            moduleService.populateRefClazzesDependencies(module);
            moduleService.calculateModuleViolations(module);
        }

        analysesRunner.executeAll(modules);

        // Convert modules to ModuleDTO
        List<ModuleDTO> modulesList = modules.stream()
                .map(moduleMapper::toDto)
                .collect(Collectors.toList());
        moduleService.saveModules(modulesList);

        return modulesList;
    }
}
