package tcc.com.viewer.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.mapstruct.ClazzMapper;
import tcc.com.viewer.mapstruct.DependencyMapper;
import tcc.com.viewer.mapstruct.ModuleMapper;
import tcc.com.viewer.services.ModuleService;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/module")
public class ModuleController {

    private final ModuleService moduleService;
    // Using MapStruct for cleaner conversion
    private ClazzMapper clazzMapper;
    private DependencyMapper dependencyMapper;
    private ModuleMapper moduleMapper;

    @PostMapping("/merge")
    public ResponseEntity<ModuleDTO> mergeModules(@RequestParam String sourceId, @RequestParam String targetId) {
        try {
            // Load existing modules
            List<ModuleDTO> modules = moduleService.getModulesFromFile();

            // Find the target and source modules by UUID
            Optional<ModuleDTO> targetModule = modules.stream()
                    .filter(module -> targetId.equals(module.id()))
                    .findFirst();

            Optional<ModuleDTO> sourceModule = modules.stream()
                    .filter(module -> sourceId.equals(module.id()))
                    .findFirst();

            // Return error if modules are not found
            if (targetModule.isEmpty() || sourceModule.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Create a new merged module
            ModuleDTO newModule = createMergedModule(targetModule.get(), sourceModule.get());

            // Create updated list: add new module, remove the source modules
            List<ModuleDTO> updatedModules = new ArrayList<>(modules);
            updatedModules.removeIf(module ->
                    module.id().equals(targetId) || module.id().equals(sourceId));
            updatedModules.add(newModule);

            // Save updated modules to file
            moduleService.saveModules(updatedModules);

            return ResponseEntity.ok(newModule);
        } catch (Exception pException) {
            log.error("e: ", pException);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ModuleDTO createMergedModule(ModuleDTO module1, ModuleDTO module2) {
        // Concatenate names with pipe separator
        String newName = module1.name() + " | " + module2.name();

        // Convert and merge classes without duplicates
        Clazz[] mergedClasses = Stream.concat(
                Arrays.stream(module1.clazzes()).map(clazzMapper::toEntity),
                Arrays.stream(module2.clazzes()).map(clazzMapper::toEntity)
        ).distinct().toArray(Clazz[]::new);

        // Convert and merge dependencies without duplicates
        Dependency[] mergedDependencies = Stream.concat(
                Arrays.stream(module1.dependencies()).map(dependencyMapper::toEntity),
                Arrays.stream(module2.dependencies()).map(dependencyMapper::toEntity)
        ).distinct().toArray(Dependency[]::new);

        Module newModule = new Module(
                UUID.randomUUID().toString(),
                newName,
                null, // refClass will be calculated later
                mergedClasses,
                mergedDependencies,
                0.0 // Similarity will be calculated later
        );
        // Recalculate similarities using ModuleService
        moduleService.calculateClassSimilarities(newModule);
        moduleService.calculateModuleSimilarity(newModule);

        return moduleMapper.toDto(newModule);
    }
}
