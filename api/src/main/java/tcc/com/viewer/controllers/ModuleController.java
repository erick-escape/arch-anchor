package tcc.com.viewer.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.dto.module.SetRefClazzesRequest;
import tcc.com.viewer.dto.module.SetRefClazzModeRequest;
import tcc.com.viewer.dto.module.SplitModuleRequest;
import tcc.com.viewer.dto.module.SplitModuleResponse;
import tcc.com.viewer.mapstruct.ClazzMapper;
import tcc.com.viewer.mapstruct.DependencyMapper;
import tcc.com.viewer.mapstruct.ModuleMapper;
import tcc.com.viewer.services.ModuleService;

import java.util.*;
import java.util.stream.Collectors;
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
			updatedModules.removeIf(module -> module.id().equals(targetId) || module.id().equals(sourceId));
			updatedModules.add(newModule);

			// Save updated modules to file
			moduleService.saveModules(updatedModules);

			return ResponseEntity.ok(newModule);
		}
		catch (Exception pException) {
			log.error("e: ", pException);
			return ResponseEntity.internalServerError().build();
		}
	}

	private ModuleDTO createMergedModule(ModuleDTO module1, ModuleDTO module2) {
		// Concatenate names with pipe separator
		String newName = module1.name() + " | " + module2.name();

		// Safe conversion and merging of classes without duplicates
		List<Clazz> mergedClasses = Stream.concat(
				Arrays.stream(Optional.ofNullable(module1.clazzes()).orElse(new ClazzResponseDTO[0]))
					.map(clazzMapper::toEntity),
				Arrays.stream(Optional.ofNullable(module2.clazzes()).orElse(new ClazzResponseDTO[0]))
					.map(clazzMapper::toEntity))
			.distinct()
			.toList();

		Module newModule = new Module(moduleService.generateNewUUID(), newName, null, // refClass
																						// will
																						// be
																						// calculated
																						// later
				null, // refClazzesDependencies will be calculated later
				null, // moduleDependencies will be calculated later
				mergedClasses, 0.0, // Similarity will be calculated later
				0.0, // avgRefClazzesSimilarity will be calculated later
				0 // violations will be calculated later
		);
		// Recalculate similarities using ModuleService
		moduleService.calculateClassSimilaritiesAndSelectRefClasses(newModule);
		moduleService.calculateAvgSimilarityWithRefClazzes(newModule);
		moduleService.calculateModuleSimilarity(newModule);
		moduleService.populateRefClazzesDependencies(newModule);
		moduleService.populateModuleDependencies(newModule);
		moduleService.calculateModuleViolations(newModule);

		return moduleMapper.toDto(newModule);
	}

	@PostMapping("/split")
	public ResponseEntity<SplitModuleResponse> splitModule(@RequestBody SplitModuleRequest request) {
		try {
			// Validate input
			if (request.moduleId() == null || request.moduleId().trim().isEmpty()) {
				return ResponseEntity.badRequest().build();
			}

			if (request.classIds() == null || request.classIds().isEmpty()) {
				return ResponseEntity.badRequest().build();
			}

			// Load existing modules
			List<ModuleDTO> modules = moduleService.getModulesFromFile();

			// Find the target module
			Optional<ModuleDTO> targetModuleOptional = modules.stream()
				.filter(module -> module.id().equals(request.moduleId()))
				.findFirst();

			if (targetModuleOptional.isEmpty()) {
				return ResponseEntity.notFound().build();
			}

			ModuleDTO targetModule = targetModuleOptional.get();

			// Validate that all classIds exist in the target module
			Set<String> moduleClassIds = Arrays.stream(targetModule.clazzes())
				.map(ClazzResponseDTO::id)
				.collect(Collectors.toSet());

			boolean allClassIdsExist = moduleClassIds.containsAll(request.classIds());

			if (!allClassIdsExist) {
				return ResponseEntity.badRequest().build();
			}

			// Split the module
			List<ModuleDTO> splitModules = moduleService.splitModule(targetModule, request.classIds());

			// Remove original module and add split modules
			List<ModuleDTO> updatedModules = modules.stream()
				.filter(module -> !module.id().equals(request.moduleId()))
				.collect(Collectors.toCollection(ArrayList::new));

			updatedModules.addAll(splitModules);

			// Save updated modules
			moduleService.saveModules(updatedModules);

			return ResponseEntity.ok(new SplitModuleResponse(splitModules));
		}
		catch (Exception pException) {
			log.error("pException --> ", pException);
			return ResponseEntity.internalServerError().build();
		}
	}

	@DeleteMapping("/delete")
	public ResponseEntity<Object> deleteModule(@RequestParam String moduleId) {
		// Load existing modules
		List<ModuleDTO> modules = moduleService.getModulesFromFile();
		// Check if module exists
		boolean moduleExists = modules.stream().anyMatch(module -> module.id().equals(moduleId));

		if (!moduleExists) {
			return ResponseEntity.notFound().build();
		}
		// Filter out the module with the matching ID
		List<ModuleDTO> updatedModules = modules.stream()
			.filter(module -> !module.id().equals(moduleId))
			.collect(Collectors.toList());

		// Save updated modules to file
		moduleService.saveModules(updatedModules);

		return ResponseEntity.noContent().build();
	}

	@PostMapping("/rename")
	public ResponseEntity<Object> renameModule(@RequestParam String moduleId, @RequestParam String newName) {
		// Load existing modules
		List<ModuleDTO> modules = moduleService.getModulesFromFile();

		// Find the module to rename
		Optional<ModuleDTO> moduleOptional = modules.stream().filter(dto -> dto.id().equals(moduleId)).findFirst();

		if (moduleOptional.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// Get the existing module DTO
		ModuleDTO oldModuleDto = moduleOptional.get();

		// Convert to entity, modify it, and convert back
		Module moduleEntity = moduleMapper.toEntity(oldModuleDto);
		moduleEntity.setName(newName);
		ModuleDTO updatedModuleDto = moduleMapper.toDto(moduleEntity);

		// Replace the old DTO in the list
		int index = modules.indexOf(oldModuleDto);
		modules.set(index, updatedModuleDto);

		// Save updated modules to file
		moduleService.saveModules(modules);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/ref-clazzes")
	public ResponseEntity<ModuleDTO> setRefClazzes(@RequestBody SetRefClazzesRequest request) {
		try {
			// Validate input
			if (request.moduleId() == null || request.moduleId().trim().isEmpty()) {
				return ResponseEntity.badRequest().build();
			}

			if (request.classIds() == null || request.classIds().isEmpty()) {
				return ResponseEntity.badRequest().build();
			}

			// Call service to set reference classes
			ModuleDTO updatedModule = moduleService.setRefClazzes(request.moduleId(), request.classIds());

			if (updatedModule == null) {
				return ResponseEntity.notFound().build();
			}

			return ResponseEntity.ok(updatedModule);
		}
		catch (Exception pException) {
			log.error("Error setting reference classes: ", pException);
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/ref-clazz-mode")
	public ResponseEntity<ModuleDTO> setRefClazzMode(@RequestBody SetRefClazzModeRequest request) {
		if (request.moduleId() == null || request.classId() == null || request.mode() == null) {
			return ResponseEntity.badRequest().build();
		}
		try {
			ModuleDTO updated = moduleService.setRefClazzMode(request.moduleId(), request.classId(), request.mode());
			return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping("/move-class")
	public ResponseEntity<Map<String, ModuleDTO>> moveClass(@RequestParam String classId,
			@RequestParam String sourceModuleId, @RequestParam String targetModuleId) {
		try {
			log.info("Moving class {} from module {} to module {}", classId, sourceModuleId, targetModuleId);

			// Load existing modules
			List<ModuleDTO> modules = moduleService.getModulesFromFile();

			// Find source and target modules
			Optional<ModuleDTO> sourceModuleOptional = modules.stream()
				.filter(module -> module.id().equals(sourceModuleId))
				.findFirst();

			Optional<ModuleDTO> targetModuleOptional = modules.stream()
				.filter(module -> module.id().equals(targetModuleId))
				.findFirst();

			if (sourceModuleOptional.isEmpty() || targetModuleOptional.isEmpty()) {
				log.error("Source or target module not found");
				return ResponseEntity.notFound().build();
			}

			ModuleDTO sourceModule = sourceModuleOptional.get();
			ModuleDTO targetModule = targetModuleOptional.get();

			// Find the class to move
			Optional<ClazzResponseDTO> classToMoveOptional = Arrays.stream(sourceModule.clazzes())
				.filter(clazz -> clazz.id().equals(classId))
				.findFirst();

			if (classToMoveOptional.isEmpty()) {
				log.error("Class {} not found in source module {}", classId, sourceModuleId);
				return ResponseEntity.badRequest().build();
			}

			ClazzResponseDTO classToMove = classToMoveOptional.get();

			// Create updated source module (remove class)
			List<Clazz> sourceClasses = Arrays.stream(sourceModule.clazzes())
				.filter(clazz -> !clazz.id().equals(classId))
				.map(clazzMapper::toEntity)
				.collect(Collectors.toList());

			Module updatedSourceModule = new Module(sourceModule.id(), sourceModule.name(), null, // will
																									// be
																									// recalculated
					null, null, sourceClasses, 0.0, 0.0, 0);

			// Recalculate source module metrics
			moduleService.calculateClassSimilaritiesAndSelectRefClasses(updatedSourceModule);
			moduleService.calculateAvgSimilarityWithRefClazzes(updatedSourceModule);
			moduleService.calculateModuleSimilarity(updatedSourceModule);
			moduleService.populateRefClazzesDependencies(updatedSourceModule);
			moduleService.populateModuleDependencies(updatedSourceModule);
			moduleService.calculateModuleViolations(updatedSourceModule);

			// Create updated target module (add class)
			List<Clazz> targetClasses = new ArrayList<>(
					Arrays.stream(targetModule.clazzes()).map(clazzMapper::toEntity).collect(Collectors.toList()));
			targetClasses.add(clazzMapper.toEntity(classToMove));

			Module updatedTargetModule = new Module(targetModule.id(), targetModule.name(), null, // will
																									// be
																									// recalculated
					null, null, targetClasses, 0.0, 0.0, 0);

			// Recalculate target module metrics
			moduleService.calculateClassSimilaritiesAndSelectRefClasses(updatedTargetModule);
			moduleService.calculateAvgSimilarityWithRefClazzes(updatedTargetModule);
			moduleService.calculateModuleSimilarity(updatedTargetModule);
			moduleService.populateRefClazzesDependencies(updatedTargetModule);
			moduleService.populateModuleDependencies(updatedTargetModule);
			moduleService.calculateModuleViolations(updatedTargetModule);

			// Update modules list
			List<ModuleDTO> updatedModules = modules.stream()
				.filter(module -> !module.id().equals(sourceModuleId) && !module.id().equals(targetModuleId))
				.collect(Collectors.toCollection(ArrayList::new));

			ModuleDTO updatedSourceDTO = moduleMapper.toDto(updatedSourceModule);
			ModuleDTO updatedTargetDTO = moduleMapper.toDto(updatedTargetModule);

			updatedModules.add(updatedSourceDTO);
			updatedModules.add(updatedTargetDTO);

			// Save updated modules
			moduleService.saveModules(updatedModules);

			Map<String, ModuleDTO> result = new HashMap<>();
			result.put("source", updatedSourceDTO);
			result.put("target", updatedTargetDTO);

			log.info("Successfully moved class {} from {} to {}", classId, sourceModuleId, targetModuleId);
			return ResponseEntity.ok(result);
		}
		catch (Exception e) {
			log.error("Error moving class", e);
			return ResponseEntity.internalServerError().build();
		}
	}

}
