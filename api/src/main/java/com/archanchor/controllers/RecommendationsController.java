package com.archanchor.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.dto.module.SplitModuleRequest;
import com.archanchor.dto.module.SplitModuleResponse;
import com.archanchor.dto.recommendations.RecommendationsResponseDTO;
import com.archanchor.services.ProjectService;
import com.archanchor.services.RecommendationsService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationsController {

	private final RecommendationsService recommendationsService;

	private final ModuleController moduleController;

	private final ProjectService projectService;

	public RecommendationsController(RecommendationsService recommendationsService, ModuleController moduleController,
			ProjectService projectService) {
		this.recommendationsService = recommendationsService;
		this.moduleController = moduleController;
		this.projectService = projectService;
	}

	@GetMapping
	public ResponseEntity<RecommendationsResponseDTO> getRecommendations() {
		log.info("GET /api/recommendations - Fetching all recommendations");
		try {
			RecommendationsResponseDTO recommendations = recommendationsService.getRecommendations();
			log.info("Successfully retrieved recommendations: {} total",
					recommendations.summary().totalRecommendations());
			return ResponseEntity.ok(recommendations);
		}
		catch (Exception e) {
			log.error("Error fetching recommendations", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/apply/split")
	public ResponseEntity<SplitModuleResponse> applySplit(@RequestBody SplitModuleRequest request) {
		log.info("POST /api/recommendations/apply/split - Applying split recommendation for module {}",
				request.moduleId());
		return refreshingRecommendations(moduleController.splitModule(request));
	}

	@PostMapping("/apply/merge")
	public ResponseEntity<ModuleDTO> applyMerge(@RequestParam String sourceId, @RequestParam String targetId) {
		log.info("POST /api/recommendations/apply/merge - Applying merge recommendation: {} -> {}", sourceId, targetId);
		return refreshingRecommendations(moduleController.mergeModules(sourceId, targetId));
	}

	@PostMapping("/apply/move")
	public ResponseEntity<Map<String, ModuleDTO>> applyMove(@RequestParam String classId,
			@RequestParam String sourceModuleId, @RequestParam String targetModuleId) {
		log.info("POST /api/recommendations/apply/move - Applying move recommendation: class {} from {} to {}", classId,
				sourceModuleId, targetModuleId);
		return refreshingRecommendations(moduleController.moveClass(classId, sourceModuleId, targetModuleId));
	}

	// The analyses keep the results of their last run. After a successful apply those
	// describe modules that no longer exist, and the applied recommendation would be
	// offered again (#19). A rejected apply changed nothing, so it refreshes nothing.
	private <T> ResponseEntity<T> refreshingRecommendations(ResponseEntity<T> applied) {
		if (applied.getStatusCode().is2xxSuccessful()) {
			projectService.refreshRecommendations();
		}
		return applied;
	}

}
