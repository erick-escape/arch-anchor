package tcc.com.viewer.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * This class indexes all available classes from the project's classpath
 * to resolve types from external libraries.
 */
public class ClasspathIndexer {
    // Map from simple class name to its fully qualified name(s)
    private final Map<String, List<String>> simpleNameToQualifiedNames = new ConcurrentHashMap<>();

    // Cache of which jar contains which class
    private final Map<String, String> classToJarMap = new ConcurrentHashMap<>();

    // Flag to indicate if the indexer has been initialized
    private boolean initialized = false;

    /**
     * Initializes the class index by scanning the classpath
     */
    public void initialize(String projectRootPath) throws IOException {
        if (initialized) return;

        System.out.println("Initializing classpath indexer...");

        // Get the system classpath
        String[] systemClasspath = System.getProperty("java.class.path").split(File.pathSeparator);

        // Add any project-specific JARs
        Set<String> classpathEntries = new HashSet<>(Arrays.asList(systemClasspath));

        // Scan for Maven/Gradle dependencies
        addMavenDependencies(projectRootPath, classpathEntries);
        addGradleDependencies(projectRootPath, classpathEntries);

        // Process each classpath entry
        for (String entry : classpathEntries) {
            File file = new File(entry);

            if (file.exists()) {
                if (file.isDirectory()) {
                    // Directory classpath entry
                    indexDirectory(file);
                } else if (entry.endsWith(".jar")) {
                    // JAR file
                    indexJarFile(file);
                }
            }
        }

        initialized = true;
        System.out.println("Classpath indexer initialized with " + simpleNameToQualifiedNames.size() + " unique class names");
    }

    private void addMavenDependencies(String projectRootPath, Set<String> classpathEntries) {
        Path mavenRepo = Paths.get(System.getProperty("user.home"), ".m2", "repository");
        if (Files.exists(mavenRepo)) {
            // Find pom.xml files in the project
            try {
                List<Path> pomFiles = Files.walk(Paths.get(projectRootPath))
                        .filter(path -> path.getFileName().toString().equals("pom.xml"))
                        .collect(Collectors.toList());

                for (Path pomPath : pomFiles) {
                    // Here you would parse the pom.xml to extract dependencies
                    // This is a simplified version - in a real implementation you'd use a Maven API
                    try {
                        String pomContent = Files.readString(pomPath);
                        // Very basic parsing - in a real implementation, use XML parsing
                        extractMavenDependenciesFromPom(pomContent, mavenRepo, classpathEntries);
                    } catch (IOException e) {
                        System.out.println("Could not read pom file: " + pomPath);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error scanning for pom.xml files: " + e.getMessage());
            }
        }
    }

    private void extractMavenDependenciesFromPom(String pomContent, Path mavenRepo, Set<String> classpathEntries) {
        // This is a very simplified version of Maven dependency resolution
        // In a real implementation, you would use Maven's APIs

        // Extract <dependency> blocks
        int start = 0;
        while ((start = pomContent.indexOf("<dependency>", start)) != -1) {
            int end = pomContent.indexOf("</dependency>", start);
            if (end == -1) break;

            String dependencyBlock = pomContent.substring(start, end + 13);

            // Extract groupId, artifactId, version
            String groupId = extractTag(dependencyBlock, "groupId");
            String artifactId = extractTag(dependencyBlock, "artifactId");
            String version = extractTag(dependencyBlock, "version");

            if (groupId != null && artifactId != null && version != null) {
                // Convert group ID to path
                String groupPath = groupId.replace('.', '/');

                // Construct path to JAR
                Path jarPath = mavenRepo.resolve(Paths.get(groupPath, artifactId, version,
                        artifactId + "-" + version + ".jar"));

                if (Files.exists(jarPath)) {
                    classpathEntries.add(jarPath.toString());
                }
            }

            start = end + 13;
        }
    }

    private String extractTag(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";

        int start = xml.indexOf(startTag);
        if (start != -1) {
            start += startTag.length();
            int end = xml.indexOf(endTag, start);
            if (end != -1) {
                return xml.substring(start, end).trim();
            }
        }
        return null;
    }

    private void addGradleDependencies(String projectRootPath, Set<String> classpathEntries) {
        // Similar implementation for Gradle
        // This would parse build.gradle files to extract dependencies
        // For brevity, we're not implementing this here
    }

    private void indexDirectory(File directory) {
        try {
            Files.walk(directory.toPath())
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(this::indexClassFile);
        } catch (IOException e) {
            System.out.println("Error indexing directory: " + directory + " - " + e.getMessage());
        }
    }

    private void indexClassFile(Path classFilePath) {
        // Convert the file path to a class name
        String relativePath = classFilePath.toString();
        if (relativePath.endsWith(".class")) {
            relativePath = relativePath.substring(0, relativePath.length() - 6); // Remove .class
            relativePath = relativePath.replace(File.separatorChar, '.');

            // Extract the simple name
            String simpleName = relativePath.substring(relativePath.lastIndexOf('.') + 1);

            // Add to the index
            simpleNameToQualifiedNames.computeIfAbsent(simpleName, k -> new ArrayList<>())
                    .add(relativePath);
        }
    }

    private void indexJarFile(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class")) {
                    // Convert to class name
                    String className = name.substring(0, name.length() - 6).replace('/', '.');

                    // Extract simple name
                    String simpleName = className.substring(className.lastIndexOf('.') + 1);

                    // Add to indices
                    simpleNameToQualifiedNames.computeIfAbsent(simpleName, k -> new ArrayList<>())
                            .add(className);

                    classToJarMap.put(className, jarFile.getName());
                }
            }
        } catch (IOException e) {
            System.out.println("Error indexing JAR file: " + jarFile + " - " + e.getMessage());
        }
    }

    /**
     * Returns a list of all possible fully qualified names for a simple class name
     */
    public List<String> resolveClassName(String simpleName) {
        return simpleNameToQualifiedNames.getOrDefault(simpleName, Collections.emptyList());
    }

    /**
     * Resolves a simple class name using a specific package hint (from wildcard import)
     */
    public String resolveWithPackageHint(String simpleName, String packageName) {
        List<String> candidates = resolveClassName(simpleName);

        if (candidates.isEmpty()) {
            // No candidates found in the classpath
            return null;
        }

        // Check if any candidate matches the package hint
        for (String candidate : candidates) {
            if (candidate.startsWith(packageName + ".")) {
                return candidate;
            }
        }

        // No match with package hint
        return null;
    }

    /**
     * Returns information about where a class is defined
     */
    public String getClassSource(String fullyQualifiedName) {
        return classToJarMap.getOrDefault(fullyQualifiedName, "Unknown");
    }

    /**
     * Checks if a class exists in the classpath
     */
    public boolean classExists(String fullyQualifiedName) {
        return classToJarMap.containsKey(fullyQualifiedName);
    }
}