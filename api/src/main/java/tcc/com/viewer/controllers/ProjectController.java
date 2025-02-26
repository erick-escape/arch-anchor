package tcc.com.viewer.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.dto.projects.ProjectDetailDTO;
import tcc.com.viewer.dto.projects.ProjectsListResponseDTO;
import tcc.com.viewer.services.ModuleService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ProjectController {
    private static final String UPLOAD_DIR = "uploads";

    @GetMapping("/projects")
    public ProjectsListResponseDTO listProjects() {
        File projectsDir = new File(UPLOAD_DIR);
        if (!projectsDir.exists() || !projectsDir.isDirectory()) {
            return new ProjectsListResponseDTO(Collections.emptyList());
        }

        List<ProjectDetailDTO> projects = Arrays.stream(Objects.requireNonNull(projectsDir.listFiles()))
                .filter(File::isDirectory)
                .map(directory -> new ProjectDetailDTO(directory.getName()))
                .collect(Collectors.toList());

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

                Path uploadPath = Paths.get(UPLOAD_DIR);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DeleteMapping("/projects/{projectName}")
    public String deleteProject(@PathVariable String projectName) {
        Path projectDir = Paths.get(UPLOAD_DIR, projectName);

        try {
            if (Files.exists(projectDir) && Files.isDirectory(projectDir)) {
                Files.walk(projectDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                return "Project " + projectName + " deleted successfully!";
            } else {
                throw new IllegalArgumentException("Project not found or is not a directory");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete project: " + e.getMessage());
        }
    }

    @PostMapping("/analyze")
    public List<ModuleDTO> analyzeProject(@RequestParam String projectName) {
        try {
            Path projectPath = Paths.get(UPLOAD_DIR, projectName);
            if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
                throw new RuntimeException("Project not found!");
            }
            ModuleService moduleService = new ModuleService();

            return moduleService.analyze(projectPath.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Project analysis failed!");
        }
    }
}
