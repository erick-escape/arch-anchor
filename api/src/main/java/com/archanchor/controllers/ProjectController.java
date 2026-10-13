package com.archanchor.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.archanchor.dto.projects.ProjectAnalysesDTO;
import com.archanchor.dto.projects.ProjectDetailDTO;
import com.archanchor.dto.projects.ProjectsListResponseDTO;
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

}
