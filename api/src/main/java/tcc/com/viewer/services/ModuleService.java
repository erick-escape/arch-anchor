package tcc.com.viewer.services;

import org.springframework.stereotype.Service;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.dependencies.DependencyDTO;
import tcc.com.viewer.dto.module.ModuleDTO;
import tcc.com.viewer.services.parsers.ParserFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModuleService {
    private final List<Module> modules = new ArrayList<>();
    private final ParserFactory parserFactory = new ParserFactory();

    // Define file extensions to consider for each language
    private static final Map<String, List<String>> LANGUAGE_EXTENSIONS = Map.of(
            "java", List.of(".java"),
            "python", List.of(".py"),
            "javascript", List.of(".js", ".ts"),
            "php", List.of(".php")
    );

    public void saveModules(List<ModuleDTO> modulesList) {
        try {
            File file = new File("modules.bin");
            ObjectOutput objectOutput = new ObjectOutputStream(new FileOutputStream(file));

            objectOutput.writeObject(modulesList);
            objectOutput.close();
        } catch (FileNotFoundException e) {
            System.out.println("file not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("teste: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<ModuleDTO> getModulesFromFile() {
        List<ModuleDTO> modulesList = Collections.emptyList();
        try {
            File file = new File("modules.bin");
            ObjectInput objectInput = new ObjectInputStream(
                    new FileInputStream(file));
            modulesList = (List<ModuleDTO>) objectInput.readObject();
            objectInput.close();
        } catch (ClassNotFoundException e) {
            System.out.println("Modules list does not exist: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }

        return modulesList;
    }

    private void calculateClassSimilarities(Module module) {
        List<Clazz> clazzes = List.of(module.getClazzes());

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

        // Set refClass as the class with the highest similarity
        if (!clazzes.isEmpty()) {
            Clazz refClass = clazzes.stream().max(Comparator.comparingDouble(Clazz::getSimilarity)).orElse(null);
            module.setRefClass(refClass.getName());
        }
    }

    private void calculateModuleSimilarity(Module module) {
        Clazz[] clazzes = module.getClazzes();

        if (clazzes.length > 0) {
            double totalSimilarity = 0.0;
            for (Clazz clazz : clazzes) {
                totalSimilarity += clazz.getSimilarity();
            }
            module.setSimilarity(totalSimilarity / clazzes.length);
        } else {
            module.setSimilarity(0.0);
        }
    }

    private double calculateSimilarity(Clazz clazz1, Clazz clazz2) {
        Set<String> deps1 = Arrays.stream(clazz1.getDependencies()).map(Dependency::getName).collect(Collectors.toSet());
        Set<String> deps2 = Arrays.stream(clazz2.getDependencies()).map(Dependency::getName).collect(Collectors.toSet());

        int a = (int) deps1.stream().filter(deps2::contains).count();
        int b = deps1.size() - a;
        int c = deps2.size() - a;
        int firstDenominator = (a + b) == 0 ? 1 : (a + b);
        int secondDenominator = (a + c) == 0 ? 1 : (a + c);

        return 0.5 * (((double) a / firstDenominator) + ((double) a / secondDenominator));
    }

    private ModuleDTO toModuleDTO(Module module) {
        return new ModuleDTO(
                module.getName(),
                module.getRefClass(),
                Arrays.stream(module.getClazzes()).map(
                        clazz -> new ClazzResponseDTO(
                                clazz.getName(),
                                Arrays.stream(clazz.getDependencies())
                                        .map(d -> new DependencyDTO(d.getName()))
                                        .toArray(DependencyDTO[]::new),
                                clazz.getSimilarity(),
                                clazz.getFirstModule(),
                                clazz.getCurrentModule()
                        )).toArray(ClazzResponseDTO[]::new),
                Arrays.stream(module.getDependencies()).map(d -> new DependencyDTO(d.getName())).toArray(DependencyDTO[]::new),
                module.getSimilarity()
        );
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
                                className,
                                dependencies.toArray(new Dependency[0]),
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

    public List<Module> getModules(String projectDirectory) throws IOException {
        Path srcPath = Paths.get(projectDirectory, "src"); // Start from 'src' directory
        if (!Files.exists(srcPath) || !Files.isDirectory(srcPath)) {
            return this.modules; // Return empty list if 'src' does not exist or is not a directory
        }

        // Clear existing modules
        this.modules.clear();

        // Find all directories that might contain source files
        Files.walk(srcPath)
                .filter(Files::isDirectory)
                .forEach(modulePath -> {
                    try {
                        List<Clazz> clazzes = getClazzes(modulePath);
                        if (!clazzes.isEmpty()) {
                            // Determine module name based on directory structure
                            String moduleName = srcPath.relativize(modulePath).toString();
                            if (moduleName.isEmpty()) {
                                moduleName = modulePath.getFileName().toString();
                            }

                            Module module = new Module(
                                    moduleName,
                                    null, // refClass will be calculated later
                                    clazzes.toArray(new Clazz[0]),
                                    new Dependency[0], // Dependencies will be calculated later
                                    0.0 // Similarity will be calculated later
                            );
                            this.modules.add(module);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return this.modules;
    }

    public List<ModuleDTO> analyze(String directoryPath) throws IOException {
        List<Module> modules = this.getModules(directoryPath);

        for (Module module : modules) {
            this.calculateClassSimilarities(module);
            this.calculateModuleSimilarity(module);
        }

        // Convert modules to ModuleDTO
        List<ModuleDTO> modulesList = modules.stream()
                .map(this::toModuleDTO)
                .collect(Collectors.toList());
        this.saveModules(modulesList);

        return modulesList;
    }
}