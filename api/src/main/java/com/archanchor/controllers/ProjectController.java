package com.archanchor.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.archanchor.domains.analysis.InvalidAnalysisSettingsException;
import com.archanchor.domains.rating.InvalidRatingWeightsException;
import com.archanchor.dto.projects.AnalysisSettingsDTO;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.dto.projects.ProjectDetailDTO;
import com.archanchor.dto.projects.ProjectsListResponseDTO;
import com.archanchor.dto.projects.RatingWeightsDTO;
import com.archanchor.services.InvalidProjectNameException;
import com.archanchor.services.ProjectNotFoundException;
import com.archanchor.services.ProjectService;
import com.archanchor.services.ProjectsDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class ProjectController {

	private final ProjectService projectService;

	private final ProjectsDirectory projectsDirectory;

	public ProjectController(ProjectService projectService, ProjectsDirectory projectsDirectory) {
		this.projectService = projectService;
		this.projectsDirectory = projectsDirectory;
	}

	@GetMapping("/projects")
	public ProjectsListResponseDTO listProjects() throws IOException {
		List<ProjectDetailDTO> projects = projectsDirectory.listProjectNames()
			.stream()
			.map(ProjectDetailDTO::new)
			.toList();
		return new ProjectsListResponseDTO(projects);
	}

	@PostMapping("/upload")
	public void postProject(@RequestParam("files") MultipartFile[] files) {
		try {
			for (MultipartFile file : files) {
				// Skip empty files if not required
				if (file.getOriginalFilename() == null) {
					System.out.println("Skipping file with no original filename.");
					continue;
				}

				Path uploadPath = projectsDirectory.root();
				if (!Files.exists(uploadPath)) {
					Files.createDirectories(uploadPath);
				}

				// Extract the relative path (subdirectories + filename)
				String relativePath = file.getOriginalFilename();

				// Resolve the full path for the file, including subdirectories
				Path filePath = uploadPath.resolve(relativePath);
				// Ensure all parent directories for this file exist
				Files.createDirectories(filePath.getParent());

				// Create the file if it does not exist
				if (!Files.exists(filePath)) {
					Files.createFile(filePath);
				}

				// Save the file to disk
				file.transferTo(Paths.get(filePath.toUri()));
			}
		}
		catch (IOException e) {
			log.error("ProjectController -> postProject: ", e);
		}
	}

	@DeleteMapping("/projects/{projectName}")
	public String deleteProject(@PathVariable String projectName) {
		try {
			if (projectsDirectory.deleteProject(projectName)) {
				projectService.forgetSettingsOf(projectName);
				return "Project " + projectName + " deleted successfully!";
			}
			else {
				throw new IllegalArgumentException("Project not found or is not a directory");
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to delete project: " + e.getMessage());
		}
	}

	@PostMapping("/analyze")
	public ProjectAnalysesDTO analyzeProject(@RequestParam String projectName) {
		try {
			Path projectPath = projectsDirectory.resolveProject(projectName);
			if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
				throw new RuntimeException("Project not found!");
			}

			return projectService.analyzeProject(projectName, projectPath.toString());
		}
		catch (Exception e) {
			log.error("ProjectController -> analyzeProject: ", e);
			throw new RuntimeException("Project analysis failed!");
		}
	}

	/** The weights of the project's election and recommendation rate (#27). */
	@GetMapping("/projects/{projectName}/rating-weights")
	public RatingWeightsDTO ratingWeights(@PathVariable String projectName) {
		requireProject(projectName);
		return RatingWeightsDTO.of(projectService.ratingWeightsOf(projectName));
	}

	/**
	 * Replaces the project's weights; if it is the analysed project, its saved modules
	 * are re-elected and the recommendations refreshed before this returns (#27).
	 */
	@PutMapping("/projects/{projectName}/rating-weights")
	public RatingWeightsDTO updateRatingWeights(@PathVariable String projectName,
			@RequestBody RatingWeightsDTO weights) {
		requireProject(projectName);
		return RatingWeightsDTO.of(projectService.updateRatingWeights(projectName, weights.toRatingWeights()));
	}

	/** What the analyses recommend and show for the project (#36). */
	@GetMapping("/projects/{projectName}/analysis-settings")
	public AnalysisSettingsDTO analysisSettings(@PathVariable String projectName) {
		requireProject(projectName);
		return AnalysisSettingsDTO.of(projectService.analysisSettingsOf(projectName));
	}

	/**
	 * Replaces the project's analysis settings; if it is the analysed project, the
	 * recommendations are refreshed before this returns (#36).
	 */
	@PutMapping("/projects/{projectName}/analysis-settings")
	public AnalysisSettingsDTO updateAnalysisSettings(@PathVariable String projectName,
			@RequestBody AnalysisSettingsDTO settings) {
		requireProject(projectName);
		return AnalysisSettingsDTO
			.of(projectService.updateAnalysisSettings(projectName, settings.toAnalysisSettings()));
	}

	@ExceptionHandler({ InvalidRatingWeightsException.class, InvalidAnalysisSettingsException.class,
			InvalidProjectNameException.class })
	ProblemDetail rejectInvalidInput(RuntimeException failure) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, failure.getMessage());
	}

	@ExceptionHandler(ProjectNotFoundException.class)
	ProblemDetail reportUnknownProject(ProjectNotFoundException failure) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, failure.getMessage());
	}

	private void requireProject(String projectName) {
		if (!Files.isDirectory(projectsDirectory.resolveProject(projectName))) {
			throw new ProjectNotFoundException(projectName);
		}
	}

}
