package tcc.com.viewer.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.dto.module.SplitModuleRequest;
import tcc.com.viewer.dto.module.SplitModuleResponse;
import tcc.com.viewer.dto.recommendations.RecommendationsResponseDTO;
import tcc.com.viewer.services.RecommendationsService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationsController {

    private final RecommendationsService recommendationsService;
    private final ModuleController moduleController;

    public RecommendationsController(RecommendationsService recommendationsService,
                                     ModuleController moduleController) {
        this.recommendationsService = recommendationsService;
        this.moduleController = moduleController;
    }

    @GetMapping
    public ResponseEntity<RecommendationsResponseDTO> getRecommendations() {
        log.info("GET /api/recommendations - Fetching all recommendations");
        try {
            RecommendationsResponseDTO recommendations = recommendationsService.getRecommendations();
            log.info("Successfully retrieved recommendations: {} total",
                    recommendations.summary().totalRecommendations());
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Error fetching recommendations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/apply/split")
    public ResponseEntity<SplitModuleResponse> applySplit(@RequestBody SplitModuleRequest request) {
        log.info("POST /api/recommendations/apply/split - Applying split recommendation for module {}",
                request.moduleId());
        return moduleController.splitModule(request);
    }

    @PostMapping("/apply/merge")
    public ResponseEntity<ModuleDTO> applyMerge(@RequestParam String sourceId, @RequestParam String targetId) {
        log.info("POST /api/recommendations/apply/merge - Applying merge recommendation: {} -> {}",
                sourceId, targetId);
        return moduleController.mergeModules(sourceId, targetId);
    }

    @PostMapping("/apply/move")
    public ResponseEntity<Map<String, ModuleDTO>> applyMove(
            @RequestParam String classId,
            @RequestParam String sourceModuleId,
            @RequestParam String targetModuleId) {
        log.info("POST /api/recommendations/apply/move - Applying move recommendation: class {} from {} to {}",
                classId, sourceModuleId, targetModuleId);
        return moduleController.moveClass(classId, sourceModuleId, targetModuleId);
    }
}
