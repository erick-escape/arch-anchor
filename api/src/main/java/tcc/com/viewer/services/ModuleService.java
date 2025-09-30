package tcc.com.viewer.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.dependencies.DependencyDTO;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.mapstruct.*;
import tcc.com.viewer.services.parsers.ParserFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ModuleService {
    private final List<Module> modules = new ArrayList<>();
    private final ParserFactory parserFactory;
    private final ModuleMapper moduleMapper = new ModuleMapperImpl();
    private final ClazzMapper clazzMapper = new ClazzMapperImpl();
    private final DependencyMapper dependencyMapper = new DependencyMapperImpl();

    public ModuleService(ParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    // Define file extensions to consider for each language
    private static final Map<String, List<String>> LANGUAGE_EXTENSIONS = Map.of(
            "java", List.of(".java"),
            "python", List.of(".py"),
            "javascript", List.of(".js", ".ts"),
            "php", List.of(".php")
    );

    public void saveModules(List<ModuleDTO> modulesList) {
        if (modulesList == null) {
            throw new IllegalArgumentException("Modules list cannot be null");
        }

        File file = new File("modules.bin");
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(file))) {
            objectOutput.writeObject(modulesList);
        } catch (IOException e) {
            throw new RuntimeException("Error saving modules to file: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<ModuleDTO> getModulesFromFile() {
        File file = new File("modules.bin");

        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        try (ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(file))) {
            return (List<ModuleDTO>) objectInput.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error reading modules: Class not found", e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading modules from file: " + e.getMessage(), e);
        }
    }

    public String generateNewUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a deterministic UUID based on a string input to prevent duplicate modules
     */
    private String generateDeterministicUUID(String input) {
        try {
            // Use a hash of the input string to generate a consistent UUID
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert the hash bytes to a UUID format
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            String hashString = sb.toString();
            // Format as UUID: 8-4-4-4-12
            return String.format("%s-%s-%s-%s-%s",
                    hashString.substring(0, 8),
                    hashString.substring(8, 12),
                    hashString.substring(12, 16),
                    hashString.substring(16, 20),
                    hashString.substring(20, 32));
        } catch (Exception e) {
            // Fallback to random UUID if hashing fails
            return UUID.randomUUID().toString();
        }
    }

    public void calculateClassSimilarities(Module module) {
        List<Clazz> clazzes = module.getClazzes();

        if (clazzes.size() > 1) {
            for (Clazz clazz : clazzes) {
                double totalSimilarity = 0.0;
                for (Clazz otherClazz : clazzes) {
                    if (!clazz.equals(otherClazz)) {
                        totalSimilarity += calculateSimilarity(clazz, otherClazz);
                    }
                }
                clazz.setSimilarity(totalSimilarity / (clazzes.size() - 1));
            }
        } else if (clazzes.size() == 1) {
            Clazz clazz = clazzes.get(0);
            clazz.setSimilarity(1.0);
        }

        // Set refClazz as the class with the highest similarity
        if (!clazzes.isEmpty()) {
            Clazz refClazz = clazzes.stream().max(Comparator.comparingDouble(Clazz::getSimilarity)).orElse(null);
            List<Clazz> refClazzes = List.of(refClazz);
            module.setRefClazzes(refClazzes);
        }
    }

    public void calculateModuleSimilarity(Module module) {
        List<Clazz> clazzes = module.getClazzes();

        if (!clazzes.isEmpty()) {
            double totalSimilarity = 0.0;
            for (Clazz clazz : clazzes) {
                totalSimilarity += clazz.getSimilarity();
            }
            module.setSimilarity(totalSimilarity / clazzes.size());
        } else {
            module.setSimilarity(0.0);
        }
    }

    private double calculateSimilarity(Clazz clazz1, Clazz clazz2) {
        Set<String> deps1 = clazz1.getDependencies().stream().map(Dependency::getPackageName).collect(Collectors.toSet());
        Set<String> deps2 = clazz2.getDependencies().stream().map(Dependency::getPackageName).collect(Collectors.toSet());

        int a = (int) deps1.stream().filter(deps2::contains).count();
        int b = deps1.size() - a;
        int c = deps2.size() - a;
        int firstDenominator = (a + b) == 0 ? 1 : (a + b);
        int secondDenominator = (a + c) == 0 ? 1 : (a + c);

        return 0.5 * (((double) a / firstDenominator) + ((double) a / secondDenominator));
    }


    public void populateModuleDependencies(Module module) {
        if (module.getClazzes() == null || module.getClazzes().isEmpty()) {
            module.setModuleDependencies(new ArrayList<>());
            return;
        }

        Map<String, Dependency> uniqueDependencies = new HashMap<>();

        for (Clazz clazz : module.getClazzes()) {
            if (clazz.getDependencies() != null) {
                for (Dependency dependency : clazz.getDependencies()) {
                    if (dependency.getPackageName() != null && !dependency.getPackageName().isEmpty()) {
                        String packageName = dependency.getPackageName();

                        if (uniqueDependencies.containsKey(packageName)) {
                            // Merge types from this dependency into the existing one
                            Dependency existingDependency = uniqueDependencies.get(packageName);
                            if (dependency.getTypes() != null) {
                                for (var type : dependency.getTypes()) {
                                    existingDependency.addType(type);
                                }
                            }
                        } else {
                            // Add new dependency
                            uniqueDependencies.put(packageName, dependency);
                        }
                    }
                }
            }
        }

        List<Dependency> allDependencies = new ArrayList<>(uniqueDependencies.values());
        module.setModuleDependencies(allDependencies);
    }

    public void populateRefClazzesDependencies(Module module) {
        if (module.getRefClazzes() == null || module.getRefClazzes().isEmpty()) {
            module.setRefClazzesDependencies(new ArrayList<>());
            return;
        }

        Map<String, Dependency> uniqueDependencies = new HashMap<>();

        for (Clazz clazz : module.getRefClazzes()) {
            if (clazz.getDependencies() != null) {
                for (Dependency dependency : clazz.getDependencies()) {
                    if (dependency.getPackageName() != null && !dependency.getPackageName().isEmpty()) {
                        String packageName = dependency.getPackageName();

                        if (uniqueDependencies.containsKey(packageName)) {
                            // Merge types from this dependency into the existing one
                            Dependency existingDependency = uniqueDependencies.get(packageName);
                            if (dependency.getTypes() != null) {
                                for (var type : dependency.getTypes()) {
                                    existingDependency.addType(type);
                                }
                            }
                        } else {
                            // Add new dependency
                            uniqueDependencies.put(packageName, dependency);
                        }
                    }
                }
            }
        }

        List<Dependency> refClazzesDependencies = new ArrayList<>(uniqueDependencies.values());
        module.setRefClazzesDependencies(refClazzesDependencies);
    }

    public List<ModuleDTO> splitModule(ModuleDTO originalModule, List<String> classIds) {
        // Convert to entity for easier manipulation
        Module originalModuleEntity = moduleMapper.toEntity(originalModule);

        // Partition classes
        Map<Boolean, List<ClazzResponseDTO>> partitionedClasses = Arrays.stream(originalModule.clazzes())
                .collect(Collectors.partitioningBy(clazz -> classIds.contains(clazz.id())));

        List<ClazzResponseDTO> retainedClasses = partitionedClasses.get(false);
        List<ClazzResponseDTO> extractedClasses = partitionedClasses.get(true);

        // Create the retained module (original with fewer classes)
        Module retainedModuleEntity = new Module();
        retainedModuleEntity.setId(originalModuleEntity.getId());
        retainedModuleEntity.setName(originalModuleEntity.getName() + "_Retained");
        retainedModuleEntity.setClazzes(retainedClasses.stream()
                .map(clazzMapper::toEntity)
                .toList());

        this.calculateClassSimilarities(retainedModuleEntity);
        this.calculateModuleSimilarity(retainedModuleEntity);
        this.populateRefClazzesDependencies(retainedModuleEntity);
        this.populateModuleDependencies(retainedModuleEntity);

        // Create the new module (with extracted classes)
        Module newModuleEntity = new Module();
        newModuleEntity.setId(generateNewUUID());
        newModuleEntity.setName(originalModuleEntity.getName() + "_Splited");
        newModuleEntity.setClazzes(extractedClasses.stream()
                .map(clazzMapper::toEntity)
                .toList());

        this.calculateClassSimilarities(newModuleEntity);
        this.calculateModuleSimilarity(newModuleEntity);
        this.populateRefClazzesDependencies(newModuleEntity);
        this.populateModuleDependencies(newModuleEntity);

        // Convert back to DTOs
        ModuleDTO retainedDto = moduleMapper.toDto(retainedModuleEntity);
        ModuleDTO newDto = moduleMapper.toDto(newModuleEntity);

        return Arrays.asList(retainedDto, newDto);
    }

    private List<Clazz> getClazzes(Path modulePath) throws IOException {
        List<Clazz> clazzes = new ArrayList<>();

        Files.list(modulePath)
                .filter(Files::isRegularFile)
                .filter(this::isSupportedSourceFile)
                .forEach(filePath -> {
                    try {
                        // Get dependencies using the appropriate parser
                        List<Dependency> dependencies = parserFactory
                                .getParser(filePath)
                                .getDependencies(filePath);

                        String fileName = filePath.getFileName().toString();
                        String className = removeFileExtension(fileName);

                        Clazz clazz = new Clazz(
                                generateNewUUID(),
                                className,
                                dependencies,
                                0.0, // Similarity will be calculated later
                                modulePath.getFileName().toString(), // firstModule
                                modulePath.getFileName().toString() // currentModule
                        );
                        clazzes.add(clazz);
                    } catch (UnsupportedOperationException e) {
                        // If the language parser is not yet implemented, log and skip
                        System.out.println("Skipping unsupported file: " + filePath);
                    }
                });

        return clazzes;
    }

    private boolean isSupportedSourceFile(Path filePath) {
        String path = filePath.toString().toLowerCase();
        return LANGUAGE_EXTENSIONS.values().stream()
                .flatMap(List::stream)
                .anyMatch(path::endsWith);
    }

    private String removeFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    /**
     * Checks if a directory is a leaf directory that directly contains Java files
     * (not just subdirectories)
     */
    private boolean isLeafDirectoryWithJavaFiles(Path directory) {
        try {
            // Check if this directory directly contains Java files

            return Files.list(directory)
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.toString().toLowerCase().endsWith(".java"));
        } catch (IOException e) {
            return false;
        }
    }

    public List<Module> getModules(String projectDirectory) throws IOException {
        Path srcPath = Paths.get(projectDirectory, "src"); // Start from 'src' directory
        if (!Files.exists(srcPath) || !Files.isDirectory(srcPath)) {
            return this.modules; // Return empty list if 'src' does not exist or is not a directory
        }

        // Clear existing modules
        this.modules.clear();

        // Find only leaf directories that actually contain Java files (not intermediate directories)
        Files.walk(srcPath)
                .filter(Files::isDirectory)
                .filter(this::isLeafDirectoryWithJavaFiles) // Only process directories that directly contain Java files
                .forEach(modulePath -> {
                    try {
                        List<Clazz> clazzes = getClazzes(modulePath);
                        if (!clazzes.isEmpty()) {
                            // Determine module fullyQualifiedName based on directory structure
                            String moduleName = modulePath.getParent().getFileName().toString() + '/' + modulePath.getFileName().toString();

                            // Generate deterministic UUID based on module path to prevent duplicates
                            String moduleId = generateDeterministicUUID(modulePath.toString());

                            // Check if module already exists to prevent duplicates
                            boolean moduleExists = this.modules.stream()
                                    .anyMatch(existingModule -> existingModule.getId().equals(moduleId));

                            if (!moduleExists) {
                                Module module = new Module(
                                        moduleId,
                                        moduleName,
                                        null, // refClazzes will be calculated later
                                        null, // refClazzesDependencies will be calculated later
                                        null, // moduleDependencies will be calculated later
                                        clazzes,
                                        0.0 // Similarity will be calculated later
                                );
                                this.modules.add(module);
                                log.info("Added new module: {} with ID: {}", moduleName, moduleId);
                            } else {
                                log.warn("Module already exists, skipping: {} with ID: {}", moduleName, moduleId);
                            }
                        }
                    } catch (IOException e) {
                        log.error("ModuleService -> getModules: ", e);
                    }
                });

        return this.modules;
    }

    public ModuleDTO setRefClazzes(String moduleId, List<String> refClazzIds) {
        // Load all modules
        List<ModuleDTO> modules = getModulesFromFile();

        // Find the target module by moduleId
        Optional<ModuleDTO> targetModuleOptional = modules.stream()
                .filter(module -> moduleId.equals(module.id()))
                .findFirst();

        if (targetModuleOptional.isEmpty()) {
            return null; // Module not found
        }

        ModuleDTO targetModule = targetModuleOptional.get();

        // Find the requested reference classes from the module's classes
        List<ClazzResponseDTO> refClazzes = Arrays.stream(targetModule.clazzes())
                .filter(clazz -> refClazzIds.contains(clazz.id()))
                .sorted((c1, c2) -> Double.compare(c2.similarity(), c1.similarity())) // Sort descending by similarity
                .toList();

        // Validate that all requested class IDs were found
        if (refClazzes.size() != refClazzIds.size()) {
            throw new IllegalArgumentException("Some requested class IDs were not found in the module");
        }

        // Calculate dependencies
        Module moduleEntity = moduleMapper.toEntity(targetModule);
        List<Clazz> modulesList = refClazzes.stream()
                .map(clazzMapper::toEntity)
                .toList();
        moduleEntity.setRefClazzes(modulesList);
        this.populateRefClazzesDependencies(moduleEntity);
        this.populateModuleDependencies(moduleEntity);

        // Create updated module with new reference classes
        ModuleDTO updatedModule = new ModuleDTO(
                targetModule.id(),
                targetModule.name(),
                refClazzes.toArray(new ClazzResponseDTO[0]),
                moduleEntity.getRefClazzesDependencies().stream()
                        .map(dependencyMapper::toDto)
                        .toArray(DependencyDTO[]::new),
                moduleEntity.getModuleDependencies().stream()
                        .map(dependencyMapper::toDto)
                        .toArray(DependencyDTO[]::new),
                targetModule.clazzes(),
                targetModule.similarity()
        );

        // Replace the module in the list
        List<ModuleDTO> updatedModules = modules.stream()
                .map(module -> module.id().equals(moduleId) ? updatedModule : module)
                .collect(Collectors.toList());

        // Save updated modules to file
        saveModules(updatedModules);

        return updatedModule;
    }
}