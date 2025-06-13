package tcc.com.viewer.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tcc.com.viewer.domains.dependency.Dependency;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class JDTParserService {

    // Regular expressions for parsing Gradle build files
    private static final Pattern GRADLE_DEP_PATTERN = Pattern.compile(
            "(implementation|api|compileOnly|runtimeOnly|testImplementation)\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern GRADLE_GROUP_ARTIFACT_VERSION = Pattern.compile(
            "([^:]+):([^:]+):([^:]+)");

    // Cache for classpath and sourcepath to avoid recalculating for each file
    private final Map<Path, String[]> projectClasspathCache = new HashMap<>();
    private final Map<Path, String[]> projectSourcepathCache = new HashMap<>();
    
    // Project-level classpath cache to avoid Maven calls per file
    private final Map<String, ProjectClasspathCache> globalProjectCache = new HashMap<>();

    // Set to track files that have been processed to avoid duplicate processing
    private final Set<Path> processedFiles = new HashSet<>();
    
    /**
     * Cache structure for project-level classpath information
     */
    private static class ProjectClasspathCache {
        final String[] classpath;
        final String[] sourcepath;
        final long timestamp;
        
        ProjectClasspathCache(String[] classpath, String[] sourcepath) {
            this.classpath = classpath;
            this.sourcepath = sourcepath;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            // Cache expires after 5 minutes
            return System.currentTimeMillis() - timestamp > 300000;
        }
    }

    /**
     * Clears the processed files cache - useful for new analysis sessions
     */
    public void clearProcessedFilesCache() {
        processedFiles.clear();
        projectClasspathCache.clear();
        projectSourcepathCache.clear();
        globalProjectCache.clear();
        System.out.println("Cleared processed files cache and all project caches");
        log.info("Cleared processed files cache and all project caches");
    }

    /**
     * Ensures classpath has minimum required entries for JDT binding resolution
     */
    private String[] ensureMinimumClasspathEntries(String[] classpath) {
        if (classpath == null || classpath.length == 0) {
            // Return a minimal classpath with just the JDK
            String javaHome = System.getProperty("java.home");
            return new String[]{
                javaHome + "/lib/rt.jar", // For Java 8 and earlier
                javaHome + "/jmods/java.base.jmod" // For Java 9+
            };
        }
        
        // Ensure we have at least 2 entries (JDT seems to expect this)
        if (classpath.length == 1) {
            String javaHome = System.getProperty("java.home");
            return new String[]{
                classpath[0],
                javaHome + "/jmods/java.base.jmod"
            };
        }
        
        return classpath;
    }

    /**
     * Ensures sourcepath has minimum required entries for JDT binding resolution
     */
    private String[] ensureMinimumSourcepathEntries(String[] sourcepath) {
        if (sourcepath == null || sourcepath.length == 0) {
            // Return at least the current directory
            return new String[]{".", "src"};
        }
        
        // Ensure we have at least 2 entries
        if (sourcepath.length == 1) {
            return new String[]{
                sourcepath[0],
                "."
            };
        }
        
        return sourcepath;
    }

    /**
     * Ensures classpath is valid and prevents index out of bounds errors
     */
    private String[] ensureValidClasspath(String[] classpath) {
        if (classpath == null || classpath.length == 0) {
            // Provide minimal valid classpath
            String javaHome = System.getProperty("java.home");
            return new String[]{
                javaHome + "/lib/rt.jar", // Java 8 and earlier
                javaHome + "/jmods/java.base.jmod", // Java 9+
                "." // Current directory
            };
        }
        
        // Filter out null or empty entries that cause index errors
        return Arrays.stream(classpath)
                .filter(entry -> entry != null && !entry.trim().isEmpty())
                .filter(entry -> {
                    try {
                        return Files.exists(Paths.get(entry)) || entry.equals(".");
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toArray(String[]::new);
    }

    /**
     * Ensures sourcepath is valid and prevents index out of bounds errors
     */
    private String[] ensureValidSourcepath(String[] sourcepath) {
        if (sourcepath == null || sourcepath.length == 0) {
            return new String[]{".", "src"};
        }
        
        // Filter out null or empty entries that cause index errors
        String[] validEntries = Arrays.stream(sourcepath)
                .filter(entry -> entry != null && !entry.trim().isEmpty())
                .filter(entry -> {
                    try {
                        return Files.exists(Paths.get(entry)) || entry.equals(".");
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toArray(String[]::new);
        
        // Ensure we have at least one valid entry
        if (validEntries.length == 0) {
            return new String[]{"."};
        }
        
        return validEntries;
    }

    /**
     * Parses a Java file and returns its AST
     */
    public CompilationUnit parseClass(Path classPath) throws IOException {
        // Note: Duplicate processing check moved to getDependencies() method
        // This allows parseClass to be reused independently if needed

        String source = Files.readString(classPath);
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // Use the latest supported JLS level

        // Set parser options with more robust error recovery
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true); // Enable binding resolution for proper type resolution
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true); // Enhanced error recovery

        // Set up compiler options with more lenient settings
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_19, options); // Adjust version as needed

        // Configure error handling to be more tolerant
        options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);
        options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
        options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.IGNORE);
        options.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.IGNORE);
        options.put(JavaCore.COMPILER_PB_UNUSED_TYPE_PARAMETER, JavaCore.IGNORE);
        options.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.IGNORE);
        options.put(JavaCore.COMPILER_PB_REDUNDANT_SUPERINTERFACE, JavaCore.IGNORE);

        parser.setCompilerOptions(options);

        // Determine the project directory being analyzed
        Path projectDir = findProjectRoot(classPath);

        // Get classpath and sourcepath using project-level caching
        ProjectClasspathCache projectCache = getOrCreateProjectCache(projectDir);
        String[] classpath = projectCache.classpath.clone(); // Clone to avoid modification
        String[] sourcepath = projectCache.sourcepath.clone();

        // Apply additional validation to prevent index out of bounds errors
        classpath = ensureMinimumClasspathEntries(classpath);
        sourcepath = ensureMinimumSourcepathEntries(sourcepath);

        log.info("=== CLASSPATH INITIALIZATION FOR {} ===", classPath.getFileName());
        log.info("Classpath entries: {}", classpath.length);
        log.info("Sourcepath entries: {}", sourcepath.length);
        
        // Log first few classpath entries for debugging
        for (int i = 0; i < Math.min(5, classpath.length); i++) {
            log.debug("CP[{}]: {}", i, classpath[i]);
        }
        if (classpath.length > 5) {
            log.debug("... and {} more classpath entries", classpath.length - 5);
        }
        
        // Always set environment with validated arrays to ensure consistent binding resolution
        parser.setEnvironment(classpath, sourcepath, null, true);
        log.debug("JDT environment configured successfully");
        
        parser.setUnitName(classPath.getFileName().toString());
        
        log.info("Parsing {} with binding resolution enabled", classPath.getFileName());

        try {
            log.info("=== PARSE ATTEMPT 1: {} with full binding resolution ===", classPath.getFileName());
            long startTime = System.currentTimeMillis();
            CompilationUnit unit = (CompilationUnit) parser.createAST(null);
            long endTime = System.currentTimeMillis();
            
            // Avoid returning null which could cause NPEs later
            if (unit != null) {
                log.info("✓ PARSE SUCCESS: {} parsed in {}ms with {} imports", 
                    classPath.getFileName(), (endTime - startTime), unit.imports().size());
                
                // Log import details for debugging binding resolution
                if (unit.imports().size() > 0) {
                    log.debug("Imports found:");
                    for (Object imp : unit.imports()) {
                        if (imp instanceof ImportDeclaration) {
                            ImportDeclaration importDecl = (ImportDeclaration) imp;
                            log.debug("  - {} {}", importDecl.getName().getFullyQualifiedName(), 
                                importDecl.isOnDemand() ? "(wildcard)" : "");
                        }
                    }
                }
                
                return unit;
            } else {
                throw new IllegalStateException("Parser returned null CompilationUnit");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // Specifically handle the "Index 1 out of bounds for length 1" error
            System.err.println("Index out of bounds error in binding resolution for " + classPath.getFileName() + ": " + e.getMessage());
            log.error("ArrayIndexOutOfBoundsException in binding resolution for {}: {}", classPath.getFileName(), e.getMessage());
            
            // Try with more conservative environment settings
            log.warn("=== PARSE ATTEMPT 2: {} with conservative binding resolution ===", classPath.getFileName());
            try {
                ASTParser conservativeParser = ASTParser.newParser(AST.getJLSLatest());
                conservativeParser.setSource(source.toCharArray());
                conservativeParser.setKind(ASTParser.K_COMPILATION_UNIT);
                conservativeParser.setResolveBindings(true);
                conservativeParser.setBindingsRecovery(true);
                conservativeParser.setStatementsRecovery(true);
                conservativeParser.setCompilerOptions(options);
                
                // Use more conservative environment - only essential JDK entries
                String[] conservativeClasspath = getConservativeClasspath();
                String[] conservativeSourcepath = new String[]{projectDir.toString(), "."};
                
                log.debug("Conservative classpath has {} entries", conservativeClasspath.length);
                conservativeParser.setEnvironment(conservativeClasspath, conservativeSourcepath, null, true);
                conservativeParser.setUnitName(classPath.getFileName().toString());
                
                long startTime = System.currentTimeMillis();
                CompilationUnit unit = (CompilationUnit) conservativeParser.createAST(null);
                long endTime = System.currentTimeMillis();
                
                if (unit != null) {
                    log.info("✓ CONSERVATIVE PARSE SUCCESS: {} parsed in {}ms", classPath.getFileName(), (endTime - startTime));
                    return unit;
                } else {
                    log.warn("✗ Conservative parsing returned null for {}", classPath.getFileName());
                }
            } catch (Exception conservativeError) {
                log.error("Conservative parsing also failed for {}: {}", classPath.getFileName(), conservativeError.getMessage());
            }
            
            // If conservative parsing fails, try fallback without binding resolution
            log.warn("Conservative parsing failed, trying fallback without binding resolution for {}", classPath.getFileName());
            try {
                ASTParser fallbackParser = ASTParser.newParser(AST.getJLSLatest());
                fallbackParser.setSource(source.toCharArray());
                fallbackParser.setKind(ASTParser.K_COMPILATION_UNIT);
                fallbackParser.setResolveBindings(false);
                fallbackParser.setStatementsRecovery(true);
                fallbackParser.setCompilerOptions(options);
                fallbackParser.setUnitName(classPath.getFileName().toString());
                
                CompilationUnit unit = (CompilationUnit) fallbackParser.createAST(null);
                if (unit != null) {
                    return unit;
                }
            } catch (Exception fallbackError) {
                log.error("Final fallback parsing also failed for {}: {}", classPath.getFileName(), fallbackError.getMessage());
            }
            
            // As absolute last resort for ArrayIndexOutOfBoundsException, create empty AST
            log.warn("Creating empty AST for {} after index bounds error", classPath.getFileName());
            AST ast = AST.newAST(AST.getJLSLatest());
            return ast.newCompilationUnit();
        } catch (Exception e) {
            System.err.println("Error parsing " + classPath.getFileName() + ": " + e.getMessage());
            log.error("Error parsing {} with binding resolution: {}", classPath.getFileName(), e.getMessage());
            log.error("Exception type: {}", e.getClass().getSimpleName());
            if (e.getStackTrace().length > 0) {
                log.error("Stack trace top: {}", e.getStackTrace()[0].toString());
            }

            // Fallback: try without binding resolution
            log.warn("Retrying {} without binding resolution", classPath.getFileName());
            try {
                ASTParser fallbackParser = ASTParser.newParser(AST.getJLSLatest());
                fallbackParser.setSource(source.toCharArray());
                fallbackParser.setKind(ASTParser.K_COMPILATION_UNIT);
                fallbackParser.setResolveBindings(false); // Disable only as fallback
                fallbackParser.setStatementsRecovery(true);
                fallbackParser.setCompilerOptions(options);
                fallbackParser.setUnitName(classPath.getFileName().toString());
                
                CompilationUnit unit = (CompilationUnit) fallbackParser.createAST(null);
                if (unit != null) {
                    return unit;
                }
            } catch (Exception fallbackError) {
                log.error("Fallback parsing also failed for {}: {}", classPath.getFileName(), fallbackError.getMessage());
            }

            // As last resort, create a minimal empty AST
            log.warn("Creating empty AST for {}", classPath.getFileName());
            AST ast = AST.newAST(AST.getJLSLatest());
            CompilationUnit emptyUnit = ast.newCompilationUnit();
            return emptyUnit;
        }
    }

    /**
     * Returns a conservative classpath with only essential JDK entries to prevent index errors
     */
    private String[] getConservativeClasspath() {
        String javaHome = System.getProperty("java.home");
        List<String> conservativeClasspath = new ArrayList<>();
        
        // Only add paths that actually exist to prevent ClasspathJar initialization errors
        Path rtJar = Paths.get(javaHome, "lib", "rt.jar");
        if (Files.exists(rtJar)) {
            conservativeClasspath.add(rtJar.toString());
            log.debug("Added rt.jar to conservative classpath: {}", rtJar);
        }
        
        // Add essential jmods for Java 9+
        String[] essentialJmods = {"java.base.jmod", "java.desktop.jmod", "java.xml.jmod"};
        Path jmodsDir = Paths.get(javaHome, "jmods");
        if (Files.exists(jmodsDir)) {
            for (String jmod : essentialJmods) {
                Path jmodPath = jmodsDir.resolve(jmod);
                if (Files.exists(jmodPath)) {
                    conservativeClasspath.add(jmodPath.toString());
                    log.debug("Added jmod to conservative classpath: {}", jmodPath);
                }
            }
        }
        
        // Add current directory as last resort
        conservativeClasspath.add(".");
        
        String[] result = conservativeClasspath.toArray(new String[0]);
        log.info("Conservative classpath created with {} entries", result.length);
        return result;
    }

    /**
     * Gets or creates project-level cache for classpath and sourcepath
     */
    private ProjectClasspathCache getOrCreateProjectCache(Path projectDir) {
        String projectKey = projectDir.toString();
        
        ProjectClasspathCache cache = globalProjectCache.get(projectKey);
        if (cache != null && !cache.isExpired()) {
            log.info("Using cached classpath for project: {}", projectKey);
            return cache;
        }
        
        log.info("Building new classpath cache for project: {}", projectKey);
        
        // Build classpath and sourcepath
        String[] classpath = getComprehensiveClassPath(projectDir);
        String[] sourcepath = getComprehensiveSourcePath(projectDir);
        
        // Ensure arrays are valid
        classpath = ensureValidClasspath(classpath);
        sourcepath = ensureValidSourcepath(sourcepath);
        
        // Create and cache the result
        ProjectClasspathCache newCache = new ProjectClasspathCache(classpath, sourcepath);
        globalProjectCache.put(projectKey, newCache);
        
        log.info("Cached classpath with {} entries and sourcepath with {} entries for project: {}", 
                classpath.length, sourcepath.length, projectKey);
        
        return newCache;
    }

    /**
     * Provides a comprehensive classpath with proper ordering to prioritize external libraries:
     * 1. External dependency JARs (Maven/Gradle) - FIRST for correct binding resolution
     * 2. JDK libraries 
     * 3. Project compiled classes - LAST to avoid incorrect type binding
     * 4. Project JAR files
     */
    private String[] getComprehensiveClassPath(Path projectDir) {
        // Use LinkedHashSet to maintain insertion order - critical for binding resolution
        Set<String> externalJars = new LinkedHashSet<>();
        Set<String> jdkLibraries = new LinkedHashSet<>();
        Set<String> projectClasses = new LinkedHashSet<>();
        Set<String> projectJars = new LinkedHashSet<>();

        try {
            log.info("Building comprehensive classpath for project: {}", projectDir);
            
            // STEP 1: Add external dependency JARs FIRST (highest priority for binding)
            if (Files.exists(projectDir.resolve("pom.xml"))) {
                System.out.println("Found Maven project. Parsing pom.xml for dependencies...");
                log.info("Found Maven project. Parsing pom.xml for dependencies...");
                parseMavenDependencies(projectDir, externalJars);
                downloadMissingDependencies(projectDir, externalJars);
            }

            if (Files.exists(projectDir.resolve("build.gradle"))) {
                System.out.println("Found Gradle project. Parsing build.gradle for dependencies...");
                parseGradleDependencies(projectDir.resolve("build.gradle"), externalJars);
            }

            if (Files.exists(projectDir.resolve("build.gradle.kts"))) {
                System.out.println("Found Kotlin Gradle project. Parsing build.gradle.kts for dependencies...");
                parseGradleDependencies(projectDir.resolve("build.gradle.kts"), externalJars);
            }

            // STEP 2: Add JDK libraries (second priority)
            addJdkLibraries(jdkLibraries);

            // STEP 3: Add project compiled classes (lower priority)
            addCompiledClasses(projectDir, projectClasses);

            // STEP 4: Add project JAR files (lowest priority)
            findAllJars(projectDir, projectJars);

        } catch (Exception e) {
            System.err.println("Error building classpath: " + e.getMessage());
            e.printStackTrace();
        }

        // Combine in the correct order for proper type binding resolution
        List<String> orderedClasspath = new ArrayList<>();
        orderedClasspath.addAll(externalJars);    // External JARs first
        orderedClasspath.addAll(jdkLibraries);    // JDK second  
        orderedClasspath.addAll(projectClasses);  // Project classes third
        orderedClasspath.addAll(projectJars);     // Project JARs last

        log.info("Classpath built with {} external JARs, {} JDK libs, {} project classes, {} project JARs", 
                externalJars.size(), jdkLibraries.size(), projectClasses.size(), projectJars.size());

        return orderedClasspath.toArray(new String[0]);
    }

    /**
     * Downloads and resolves Maven dependencies using Maven Resolver API
     */
    private void downloadMissingDependencies(Path projectDir, Set<String> classpath) {
        // First, check if there's a pom.xml file
        Path pomFile = projectDir.resolve("pom.xml");
        if (!Files.exists(pomFile)) {
            // If no pom.xml, check for lib directory as fallback
            Path libDir = projectDir.resolve("lib");
            if (Files.exists(libDir) && Files.isDirectory(libDir)) {
                try {
                    Files.walk(libDir)
                            .filter(path -> path.toString().endsWith(".jar"))
                            .forEach(path -> classpath.add(path.toString()));
                } catch (IOException e) {
                    System.err.println("Error scanning lib directory: " + e.getMessage());
                }
            }
            return;
        }

        // Try three different approaches for dependency resolution, in order of preference:
        // 1. Use Maven Resolver API (Programmatic)
        try {
            resolveDependenciesWithMavenAPI(pomFile, classpath);
            System.out.println("Successfully resolved dependencies using Maven Resolver API.");

            // Even if the Maven API works, still ensure we have the Spring Boot dependencies
            addSpringBootDependencies(projectDir, classpath);
            return;
        } catch (Exception e) {
            System.out.println("Maven API resolution failed: " + e.getMessage());
            System.out.println("Falling back to process-based resolution...");
        }

        // 2. Use Maven CLI (Process-based)
        try {
            if (resolveDependenciesWithMavenProcess(projectDir, pomFile, classpath)) {
                System.out.println("Successfully resolved dependencies using Maven CLI process.");

                // Even if the Maven process works, still ensure we have the Spring Boot dependencies
                addSpringBootDependencies(projectDir, classpath);
                return;
            }
        } catch (Exception e) {
            System.out.println("Maven process resolution failed: " + e.getMessage());
            System.out.println("Falling back to manual resolution...");
        }

        // 3. Manual dependency resolution (fallback)
        System.out.println("Using manual dependency resolution as fallback.");
        parseMavenDependencies(projectDir, classpath);
        addSpringBootDependencies(projectDir, classpath);
    }

    /**
     * Resolves dependencies using Maven Resolver API programmatically
     */
    private void resolveDependenciesWithMavenAPI(Path pomFile, Set<String> classpath) throws Exception {
        try {
            // Rather than implementing the complex Maven Resolver API directly,
            // we'll use the Maven CLI in a more controlled way

            // Create temporary file to store the classpath
            Path tempFile = Files.createTempFile("maven-classpath-", ".txt");

            // Build Maven command with proper parameters
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "mvn",
                    "dependency:build-classpath",
                    "-Dmdep.outputFile=" + tempFile.toString(),
                    "-f", pomFile.toString()
            );

            // Hide Maven output noise
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            System.out.println("Executing Maven to resolve dependencies: " +
                    String.join(" ", processBuilder.command()));

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new Exception("Maven process failed with exit code: " + exitCode);
            }

            // Read generated classpath file
            if (Files.exists(tempFile) && Files.size(tempFile) > 0) {
                String mavenClasspath = Files.readString(tempFile);
                String[] classpathEntries = mavenClasspath.split(System.getProperty("path.separator"));

                int count = 0;
                for (String entry : classpathEntries) {
                    if (!entry.trim().isEmpty()) {
                        classpath.add(entry.trim());
                        count++;
                    }
                }

                System.out.println("Added " + count + " Maven dependencies to classpath");

                // Delete the temporary file
                Files.deleteIfExists(tempFile);
            } else {
                throw new Exception("Maven classpath file not created or empty");
            }
        } catch (Exception e) {
            System.err.println("Error using Maven Resolver API: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Resolves dependencies by invoking the Maven CLI as a separate process
     */
    private boolean resolveDependenciesWithMavenProcess(Path projectDir, Path pomFile, Set<String> classpath) {
        try {
            // Execute Maven dependency:build-classpath to get the classpath
            Path classpathFile = projectDir.resolve(".classpath-file");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "mvn",
                    "dependency:build-classpath",
                    "-Dmdep.outputFile=" + classpathFile,
                    "-f", pomFile.toString()
            );

            System.out.println("Executing: " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(classpathFile)) {
                // Read the generated classpath file
                String mavenClasspath = Files.readString(classpathFile);
                String[] classpathEntries = mavenClasspath.split(System.getProperty("path.separator"));

                int count = 0;
                for (String entry : classpathEntries) {
                    if (!entry.trim().isEmpty()) {
                        classpath.add(entry.trim());
                        count++;
                    }
                }

                System.out.println("Added " + count + " Maven dependencies to classpath");

                // Delete the temporary file
                Files.deleteIfExists(classpathFile);
                return true;
            } else {
                System.err.println("Maven process exited with code: " + exitCode +
                        " or classpath file not created");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error executing Maven process: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds Spring Boot dependencies if the project is a Spring Boot project
     */
    private void addSpringBootDependencies(Path projectDir, Set<String> classpath) {
        try {
            // Check if the project is a Spring Boot project
            Path pomFile = projectDir.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                String pomContent = Files.readString(pomFile);

                // Check if this is a Spring Boot project by looking for Spring Boot parent or dependencies
                boolean isSpringBootProject = pomContent.contains("spring-boot-starter-parent") ||
                        pomContent.contains("spring-boot-starter");

                if (isSpringBootProject) {
                    System.out.println("Detected Spring Boot project. Adding Spring Boot dependencies...");

                    // Extract Spring Boot version from pom.xml
                    String springBootVersion = extractSpringBootVersion(pomContent);
                    if (springBootVersion == null) {
                        springBootVersion = "3.0.5"; // Default if unable to extract
                    }

                    System.out.println("Using Spring Boot version: " + springBootVersion);

                    // Add essential Spring Boot JARs
                    String userHome = System.getProperty("user.home");
                    Path m2Repo = Paths.get(userHome, ".m2", "repository");

                    // Core Spring Boot annotations and classes
                    addMavenJarToClasspath(m2Repo, "org/springframework/boot", "spring-boot", springBootVersion, classpath);
                    addMavenJarToClasspath(m2Repo, "org/springframework/boot", "spring-boot-autoconfigure", springBootVersion, classpath);
                    addMavenJarToClasspath(m2Repo, "org/springframework/boot", "spring-boot-starter", springBootVersion, classpath);

                    // Core Spring Framework dependencies
                    String springVersion = extractSpringVersion(pomContent);
                    if (springVersion == null) {
                        // Estimate Spring version based on Spring Boot version
                        if (springBootVersion.startsWith("3.")) {
                            springVersion = "6.0.0"; // Spring Boot 3.x uses Spring 6.x
                        } else {
                            springVersion = "5.3.0"; // Spring Boot 2.x uses Spring 5.x
                        }
                    }

                    System.out.println("Using Spring Framework version: " + springVersion);

                    // Add core Spring Framework JARs
                    addMavenJarToClasspath(m2Repo, "org/springframework", "spring-core", springVersion, classpath);
                    addMavenJarToClasspath(m2Repo, "org/springframework", "spring-context", springVersion, classpath);
                    addMavenJarToClasspath(m2Repo, "org/springframework", "spring-beans", springVersion, classpath);
                    addMavenJarToClasspath(m2Repo, "org/springframework", "spring-web", springVersion, classpath);
                }
            }
        } catch (Exception e) {
            System.err.println("Error adding Spring Boot dependencies: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to extract Spring Boot version from pom.xml
     */
    private String extractSpringBootVersion(String pomContent) {
        // Try to extract from parent
        Pattern parentPattern = Pattern.compile(
                "<parent>\\s*<groupId>org\\.springframework\\.boot</groupId>\\s*<artifactId>spring-boot-starter-parent</artifactId>\\s*<version>([^<]+)</version>");
        Matcher parentMatcher = parentPattern.matcher(pomContent);
        if (parentMatcher.find()) {
            return parentMatcher.group(1);
        }

        // Try to extract from properties
        Pattern propertiesPattern = Pattern.compile("<spring-boot\\.version>([^<]+)</spring-boot\\.version>");
        Matcher propertiesMatcher = propertiesPattern.matcher(pomContent);
        if (propertiesMatcher.find()) {
            return propertiesMatcher.group(1);
        }

        // Try to extract from dependency
        Pattern depPattern = Pattern.compile(
                "<groupId>org\\.springframework\\.boot</groupId>\\s*<artifactId>spring-boot[^<]*</artifactId>\\s*<version>([^<]+)</version>");
        Matcher depMatcher = depPattern.matcher(pomContent);
        if (depMatcher.find()) {
            return depMatcher.group(1);
        }

        return null;
    }

    /**
     * Helper method to extract Spring Framework version from pom.xml
     */
    private String extractSpringVersion(String pomContent) {
        // Try to extract from properties
        Pattern propertiesPattern = Pattern.compile("<spring-framework\\.version>([^<]+)</spring-framework\\.version>");
        Matcher propertiesMatcher = propertiesPattern.matcher(pomContent);
        if (propertiesMatcher.find()) {
            return propertiesMatcher.group(1);
        }

        // Try to extract from dependency
        Pattern depPattern = Pattern.compile(
                "<groupId>org\\.springframework</groupId>\\s*<artifactId>spring[^<]*</artifactId>\\s*<version>([^<]+)</version>");
        Matcher depMatcher = depPattern.matcher(pomContent);
        if (depMatcher.find()) {
            return depMatcher.group(1);
        }

        return null;
    }

    /**
     * Helper method to add a Maven JAR to the classpath
     */
    private void addMavenJarToClasspath(Path m2Repo, String groupPath, String artifactId, String version, Set<String> classpath) {
        // First, check for the specific version
        Path jarPath = m2Repo.resolve(Paths.get(groupPath, artifactId, version,
                artifactId + "-" + version + ".jar"));

        if (Files.exists(jarPath)) {
            classpath.add(jarPath.toString());
            System.out.println("Added dependency: " + jarPath);
            return;
        }

        // If specific version doesn't exist, try to find any version
        try {
            Path artifactDir = m2Repo.resolve(Paths.get(groupPath, artifactId));
            if (Files.exists(artifactDir)) {
                Optional<Path> latestVersion = Files.list(artifactDir)
                        .filter(Files::isDirectory)
                        .max(Comparator.comparing(Path::toString));

                if (latestVersion.isPresent()) {
                    Path latestJar = latestVersion.get().resolve(
                            artifactId + "-" + latestVersion.get().getFileName().toString() + ".jar");

                    if (Files.exists(latestJar)) {
                        classpath.add(latestJar.toString());
                        System.out.println("Added alternative version: " + latestJar);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error finding jar for " + artifactId + ": " + e.getMessage());
        }
    }

    /**
     * Parses Maven pom.xml file to extract dependencies
     */
    private void parseMavenDependencies(Path projectDir, Set<String> classpath) {
        Path pomFile = projectDir.resolve("pom.xml");
        if (!Files.exists(pomFile)) {
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile.toFile());
            doc.getDocumentElement().normalize();

            // First, extract properties to resolve placeholders
            Map<String, String> properties = extractMavenProperties(doc);

            // Get all dependencies
            NodeList dependencyNodes = doc.getElementsByTagName("dependency");

            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node node = dependencyNodes.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String groupId = getElementTextContent(element, "groupId");
                    String artifactId = getElementTextContent(element, "artifactId");
                    String version = getElementTextContent(element, "version");

                    // Resolve properties in values
                    groupId = resolveMavenProperty(groupId, properties);
                    artifactId = resolveMavenProperty(artifactId, properties);
                    version = resolveMavenProperty(version, properties);

                    // Skip dependencies with unresolved placeholders
                    if (groupId == null || artifactId == null || version == null ||
                            groupId.contains("${") || artifactId.contains("${") || version.contains("${")) {
                        System.out.println("Skipping dependency with unresolved placeholder: " +
                                groupId + ":" + artifactId + ":" + version);
                        continue;
                    }

                    // Ignore test scope dependencies unless explicitly included
                    String scope = getElementTextContent(element, "scope");
                    if ("test".equals(scope)) {
                        continue;
                    }

                    addMavenDependencyToClasspath(groupId, artifactId, version, classpath);

                    // For transitive dependencies, check for parent projects
                    try {
                        addTransitiveDependencies(groupId, artifactId, version, classpath);
                    } catch (Exception e) {
                        System.out.println("Error resolving transitive dependencies: " + e.getMessage());
                    }
                }
            }

            // Check for parent POM
            NodeList parentNodes = doc.getElementsByTagName("parent");
            if (parentNodes.getLength() > 0) {
                Node parentNode = parentNodes.item(0);
                if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element parentElement = (Element) parentNode;

                    String groupId = getElementTextContent(parentElement, "groupId");
                    String artifactId = getElementTextContent(parentElement, "artifactId");
                    String version = getElementTextContent(parentElement, "version");

                    // Resolve properties
                    groupId = resolveMavenProperty(groupId, properties);
                    artifactId = resolveMavenProperty(artifactId, properties);
                    version = resolveMavenProperty(version, properties);

                    if (groupId != null && artifactId != null && version != null &&
                            !groupId.contains("${") && !artifactId.contains("${") && !version.contains("${")) {
                        addMavenDependencyToClasspath(groupId, artifactId, version, classpath);
                    }
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("Error parsing pom.xml: " + e.getMessage());
        }
    }

    /**
     * Extracts properties from a Maven POM file
     */
    private Map<String, String> extractMavenProperties(Document doc) {
        Map<String, String> properties = new HashMap<>();

        // Add Maven standard properties
        properties.put("project.groupId", getTextContent(doc.getElementsByTagName("groupId")));
        properties.put("project.artifactId", getTextContent(doc.getElementsByTagName("artifactId")));
        properties.put("project.version", getTextContent(doc.getElementsByTagName("version")));

        // Extract custom properties
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            Node propertiesNode = propertiesNodes.item(0);
            if (propertiesNode.getNodeType() == Node.ELEMENT_NODE) {
                Element propertiesElement = (Element) propertiesNode;
                NodeList propertyNodes = propertiesElement.getChildNodes();

                for (int i = 0; i < propertyNodes.getLength(); i++) {
                    Node propertyNode = propertyNodes.item(i);
                    if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                        String name = propertyNode.getNodeName();
                        String value = propertyNode.getTextContent();
                        properties.put(name, value);
                    }
                }
            }
        }

        return properties;
    }

    /**
     * Gets text content from the first node in a NodeList
     */
    private String getTextContent(NodeList nodeList) {
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Resolves Maven property placeholders
     */
    private String resolveMavenProperty(String value, Map<String, String> properties) {
        if (value == null) return null;

        if (value.contains("${")) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (value.contains(placeholder)) {
                    value = value.replace(placeholder, entry.getValue());
                }
            }
        }

        return value;
    }

    /**
     * Adds transitive dependencies from a Maven dependency
     */
    private void addTransitiveDependencies(String groupId, String artifactId, String version, Set<String> classpath) {
        // This is a simplified version that would need to be expanded for a real implementation
        // In practice, you'd need to download and parse the POM of each dependency

        // Common framework dependencies to include
        if (groupId.equals("org.springframework.boot") && artifactId.equals("spring-boot-starter")) {
            // Add core Spring dependencies
            addMavenDependencyToClasspath("org.springframework", "spring-core", "5.3.10", classpath);
            addMavenDependencyToClasspath("org.springframework", "spring-context", "5.3.10", classpath);
            addMavenDependencyToClasspath("org.springframework", "spring-beans", "5.3.10", classpath);
        }

        if (groupId.equals("org.springframework.boot") && artifactId.equals("spring-boot-starter-web")) {
            // Add web dependencies
            addMavenDependencyToClasspath("org.springframework", "spring-web", "5.3.10", classpath);
            addMavenDependencyToClasspath("org.springframework", "spring-webmvc", "5.3.10", classpath);
            addMavenDependencyToClasspath("jakarta.servlet", "jakarta.servlet-api", "5.0.0", classpath);
        }

        if (groupId.equals("org.springframework.boot") && artifactId.equals("spring-boot-starter-data-jpa")) {
            // Add JPA dependencies
            addMavenDependencyToClasspath("org.hibernate", "hibernate-core", "5.6.5.Final", classpath);
            addMavenDependencyToClasspath("jakarta.persistence", "jakarta.persistence-api", "3.0.0", classpath);
            addMavenDependencyToClasspath("jakarta.transaction", "jakarta.transaction-api", "2.0.0", classpath);
        }
    }

    /**
     * Helper method to get text content of an XML element
     */
    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Adds a Maven dependency to the classpath
     */
    private void addMavenDependencyToClasspath(String groupId, String artifactId, String version, Set<String> classpath) {
        if (groupId == null || artifactId == null || version == null) {
            return;
        }

        // First, check project's lib directory
        Path projectLibPath = Paths.get("uploads").resolve(Paths.get("lib"));
        if (Files.exists(projectLibPath)) {
            try {
                Path jarInLib = Files.walk(projectLibPath)
                        .filter(path -> path.toString().endsWith(".jar"))
                        .filter(path -> path.getFileName().toString().contains(artifactId + "-" + version))
                        .findFirst().orElse(null);

                if (jarInLib != null) {
                    classpath.add(jarInLib.toString());
                    System.out.println("Added project lib dependency: " + jarInLib);
                    return; // Found in project, no need to check .m2
                }
            } catch (IOException e) {
                System.err.println("Error searching project lib: " + e.getMessage());
            }
        }

        // Then check .m2 repository
        String userHome = System.getProperty("user.home");
        Path m2Path = Paths.get(userHome, ".m2", "repository");

        // Convert group ID to path
        String groupPath = groupId.replace('.', '/');

        // Construct different possible paths to the JAR file (handle different naming conventions)
        List<Path> possibleJarPaths = new ArrayList<>();

        // Standard Maven JAR path
        possibleJarPaths.add(m2Path.resolve(Paths.get(groupPath, artifactId, version,
                artifactId + "-" + version + ".jar")));

        // Check for classifier variants
        possibleJarPaths.add(m2Path.resolve(Paths.get(groupPath, artifactId, version,
                artifactId + "-" + version + "-all.jar")));
        possibleJarPaths.add(m2Path.resolve(Paths.get(groupPath, artifactId, version,
                artifactId + "-" + version + "-jre.jar")));

        // Check for non-standard but common naming patterns
        possibleJarPaths.add(m2Path.resolve(Paths.get(groupPath, artifactId, version,
                artifactId + ".jar")));

        boolean found = false;
        for (Path jarPath : possibleJarPaths) {
            if (Files.exists(jarPath)) {
                classpath.add(jarPath.toString());
                System.out.println("Added Maven dependency: " + groupId + ":" + artifactId + ":" + version);
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Maven dependency not found in any of the expected locations: " +
                    groupId + ":" + artifactId + ":" + version);

            // Check for similar versions
            try {
                Path artifactDir = m2Path.resolve(Paths.get(groupPath, artifactId));
                if (Files.exists(artifactDir)) {
                    Optional<Path> latestVersion = Files.list(artifactDir)
                            .filter(Files::isDirectory)
                            .max(Comparator.comparing(Path::toString));

                    if (latestVersion.isPresent()) {
                        Path latestJar = latestVersion.get().resolve(
                                artifactId + "-" + latestVersion.get().getFileName().toString() + ".jar");

                        if (Files.exists(latestJar)) {
                            classpath.add(latestJar.toString());
                            System.out.println("Using alternative version: " + latestJar);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error finding alternative versions: " + e.getMessage());
            }
        }
    }

    /**
     * Parses Gradle build files to extract dependencies
     */
    private void parseGradleDependencies(Path gradleFile, Set<String> classpath) {
        if (!Files.exists(gradleFile)) {
            return;
        }

        try {
            String content = Files.readString(gradleFile);
            Matcher depMatcher = GRADLE_DEP_PATTERN.matcher(content);

            while (depMatcher.find()) {
                String dependency = depMatcher.group(2);
                Matcher gavMatcher = GRADLE_GROUP_ARTIFACT_VERSION.matcher(dependency);

                if (gavMatcher.find()) {
                    String groupId = gavMatcher.group(1);
                    String artifactId = gavMatcher.group(2);
                    String version = gavMatcher.group(3);

                    // Add to both Maven repo and Gradle cache
                    addMavenDependencyToClasspath(groupId, artifactId, version, classpath);
                    addGradleCacheDependency(groupId, artifactId, version, classpath);

                    // Add transitive dependencies
                    addTransitiveDependencies(groupId, artifactId, version, classpath);
                }
            }

        } catch (IOException e) {
            System.err.println("Error parsing Gradle file: " + e.getMessage());
        }
    }

    /**
     * Adds a dependency from the Gradle cache to the classpath
     */
    private void addGradleCacheDependency(String groupId, String artifactId, String version, Set<String> classpath) {
        String userHome = System.getProperty("user.home");

        // Common Gradle cache locations
        List<Path> gradleCachePaths = Arrays.asList(
                Paths.get(userHome, ".gradle", "caches", "modules-2", "files-2.1", groupId, artifactId, version),
                Paths.get(userHome, ".gradle", "caches", "transforms-3")
        );

        for (Path cachePath : gradleCachePaths) {
            if (!Files.exists(cachePath)) {
                continue;
            }

            try {
                Files.walkFileTree(cachePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().endsWith(".jar") &&
                                file.getFileName().toString().contains(artifactId) &&
                                !file.toString().contains("-sources.jar") &&
                                !file.toString().contains("-javadoc.jar")) {
                            classpath.add(file.toString());
                            System.out.println("Added Gradle dependency: " + file);
                            return FileVisitResult.TERMINATE; // Found what we need
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                System.err.println("Error searching Gradle cache: " + e.getMessage());
            }
        }
    }

    /**
     * Adds compiled classes directories to the classpath
     */
    private void addCompiledClasses(Path projectDir, Set<String> classpath) {
        // Maven project structure
        Path mavenClasses = projectDir.resolve(Paths.get("target", "classes"));
        if (Files.exists(mavenClasses)) {
            classpath.add(mavenClasses.toString());
        }

        // Gradle project structure
        Path gradleClasses = projectDir.resolve(Paths.get("build", "classes", "java", "main"));
        if (Files.exists(gradleClasses)) {
            classpath.add(gradleClasses.toString());
        }

        // Also check general build/classes directory
        Path simpleBuildClasses = projectDir.resolve(Paths.get("build", "classes"));
        if (Files.exists(simpleBuildClasses)) {
            try {
                Files.walkFileTree(simpleBuildClasses, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        classpath.add(dir.toString());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                System.err.println("Error searching build classes: " + e.getMessage());
            }
        }
    }

    /**
     * Recursively finds all JAR files in the given directory and adds them to the classpath
     */
    private void findAllJars(Path directory, Set<String> classpath) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".jar") &&
                        !file.toString().contains("-sources.jar") &&
                        !file.toString().contains("-javadoc.jar")) {
                    classpath.add(file.toString());
                    System.out.println("Added JAR to classpath: " + file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Skip directories that typically don't contain relevant JARs
                String dirName = dir.getFileName().toString();
                if (dirName.equals("src") || dirName.equals("test") ||
                        dirName.equals(".git") || dirName.equals("target")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Adds JDK libraries to the classpath
     */
    private void addJdkLibraries(Set<String> classpath) throws IOException {
        String javaHome = System.getProperty("java.home");
        Path jmodDir = Paths.get(javaHome, "jmods");
        Path libDir = Paths.get(javaHome, "lib");

        // Add JDK modules if available (Java 9+)
        if (Files.exists(jmodDir)) {
            Files.walkFileTree(jmodDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".jmod")) {
                        classpath.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // Add JDK JAR files
        if (Files.exists(libDir)) {
            Files.walkFileTree(libDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".jar")) {
                        classpath.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // For Java 8 and earlier, add rt.jar explicitly
        Path rtJar = Paths.get(javaHome, "lib", "rt.jar");
        if (Files.exists(rtJar)) {
            classpath.add(rtJar.toString());
        }
    }

    /**
     * Gets source paths for the project being analyzed
     */
    private String[] getComprehensiveSourcePath(Path projectDir) {
        Set<String> sourcePath = new HashSet<>();

        try {
            // Maven standard source directories
            addIfExists(sourcePath, projectDir.resolve(Paths.get("src", "main", "java")));
            addIfExists(sourcePath, projectDir.resolve(Paths.get("src", "test", "java")));

            // Gradle standard source directories
            addIfExists(sourcePath, projectDir.resolve(Paths.get("src", "main", "java")));
            addIfExists(sourcePath, projectDir.resolve(Paths.get("src", "test", "java")));

            // Traditional Java source directory
            addIfExists(sourcePath, projectDir.resolve("src"));

            // Find all directories containing Java files
            Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip some directories
                    String dirName = dir.getFileName().toString();
                    if (dirName.equals("target") || dirName.equals("build") ||
                            dirName.equals(".git") || dirName.equals("lib") ||
                            dirName.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    // Check if directory contains Java files directly
                    try {
                        if (Files.list(dir).anyMatch(path -> path.toString().endsWith(".java"))) {
                            sourcePath.add(dir.toString());
                        }
                    } catch (IOException e) {
                        // Ignore errors
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            System.err.println("Error scanning for source directories: " + e.getMessage());
        }

        return sourcePath.toArray(new String[0]);
    }

    /**
     * Helper method to add a path to the source path if it exists
     */
    private void addIfExists(Set<String> paths, Path path) {
        if (Files.exists(path)) {
            paths.add(path.toString());
        }
    }

    /**
     * Finds the project root by looking for build files (pom.xml or build.gradle)
     */
    private Path findProjectRoot(Path classPath) {
        Path current = classPath.getParent();
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml")) ||
                    Files.exists(current.resolve("build.gradle")) ||
                    Files.exists(current.resolve("build.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        // If no build file found, return the base uploads directory with safe parsing
        try {
            String pathStr = classPath.toString();
            if (pathStr.contains("uploads")) {
                String[] uploadsParts = pathStr.split("uploads[/\\\\]");
                if (uploadsParts.length > 1) {
                    String[] projectParts = uploadsParts[1].split("[/\\\\]");
                    if (projectParts.length > 0) {
                        return Paths.get("uploads").resolve(projectParts[0]);
                    }
                }
            }
            // Last resort: use the directory 3 levels up from the file
            Path fallback = classPath;
            for (int i = 0; i < 3 && fallback.getParent() != null; i++) {
                fallback = fallback.getParent();
            }
            return fallback;
        } catch (Exception e) {
            System.err.println("Error parsing project root path: " + e.getMessage());
            // Ultimate fallback: return the parent directory
            return classPath.getParent() != null ? classPath.getParent() : Paths.get(".");
        }
    }

    /**
     * Extracts all dependencies from a Java class file
     */
    public List<Dependency> getDependencies(Path classPath) {
        log.info("=== getDependencies() called for: {}", classPath);
        
        // If we've already processed this file in this session, return cached result
        if (processedFiles.contains(classPath)) {
            System.out.println("File already processed by getDependencies: " + classPath);
            log.warn("DUPLICATE: File already processed by getDependencies: {}", classPath);
            return new ArrayList<>();
        }

        // Mark this file as processed before starting processing
        processedFiles.add(classPath);
        log.info("Processing file for first time: {}", classPath);

        TypeDependencyVisitor visitor = new TypeDependencyVisitor();

        try {
            System.out.println("Extracting dependencies from: " + classPath);
            log.info("Extracting dependencies from: {}", classPath);

            CompilationUnit cu = parseClass(classPath);

            // Create a visitor to collect type references with error handling
            try {
                // The cu should never be null now due to our improvements in parseClass
                cu.accept(visitor);
            } catch (Exception e) {
                System.err.println("Error while visiting AST for " + classPath + ": " + e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("Index") &&
                        e.getMessage().contains("out of bounds")) {

                    System.out.println("Detected array index error, using alternative dependency extraction for: " + classPath);

                    // Extract imports directly from the AST
                    extractImportsDirectly(cu, visitor);

                    // Also try to extract from source code as additional fallback
                    try {
                        String source = Files.readString(classPath);
                        extractDependenciesFromSource(source, visitor, classPath);
                    } catch (Exception sourceEx) {
                        System.err.println("Error extracting from source: " + sourceEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing file " + classPath + ": " + e.getMessage());

            // Last-resort fallback: try direct source parsing
            try {
                String source = Files.readString(classPath);
                extractDependenciesFromSource(source, visitor, classPath);
            } catch (Exception sourceEx) {
                System.err.println("Error extracting from source as fallback: " + sourceEx.getMessage());
            }
        }

        List<Dependency> dependencies = visitor.getDependencies();
        
        log.info("=== FINAL BINDING RESOLUTION RESULTS FOR {} ===", classPath.getFileName());
        log.info("Total dependencies extracted: {}", dependencies.size());
        
        // Group dependencies by package for better visibility
        Map<String, List<String>> dependenciesByPackage = new LinkedHashMap<>();
        for (Dependency dep : dependencies) {
            String name = dep.getName();
            String packageName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : "default";
            dependenciesByPackage.computeIfAbsent(packageName, k -> new ArrayList<>()).add(name);
        }
        
        // Log dependencies by package
        for (Map.Entry<String, List<String>> entry : dependenciesByPackage.entrySet()) {
            String packageName = entry.getKey();
            List<String> types = entry.getValue();
            
            if (packageName.startsWith("jakarta.") || packageName.startsWith("lombok.") || 
                packageName.startsWith("org.springframework.")) {
                log.info("✓ External Library [{}]: {}", packageName, String.join(", ", types));
            } else if (packageName.startsWith("java.")) {
                log.debug("  Standard Library [{}]: {}", packageName, String.join(", ", types));
            } else {
                log.info("  Project [{}]: {}", packageName, String.join(", ", types));
            }
        }
        
        return dependencies;
    }

    /**
     * Extracts dependencies directly from source code using regex
     */
    private void extractDependenciesFromSource(String source, TypeDependencyVisitor visitor, Path classPath) {
        try {
            // Extract package
            Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+);");
            Matcher packageMatcher = packagePattern.matcher(source);
            if (packageMatcher.find()) {
                String packageName = packageMatcher.group(1);
                visitor.addDependencyIfNotExists(new Dependency(packageName));
            }

            // Extract imports
            Pattern importPattern = Pattern.compile("import\\s+([\\w.]+)(\\*)?;");
            Matcher importMatcher = importPattern.matcher(source);
            while (importMatcher.find()) {
                String importName = importMatcher.group(1);
                boolean isWildcard = importMatcher.group(2) != null;

                if (isWildcard) {
                    visitor.addOnDemandImport(importName);
                } else {
                    String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
                    visitor.addImport(simpleName, importName);
                    visitor.addDependencyIfNotExists(new Dependency(importName));
                }
            }

            // Extract class, annotation and interface references
            Pattern typePattern = Pattern.compile(
                    "(?:@|extends|implements|class|interface|enum)\\s+([A-Z][A-Za-z0-9_]*)");
            Matcher typeMatcher = typePattern.matcher(source);
            while (typeMatcher.find()) {
                String typeName = typeMatcher.group(1);
                visitor.addDependencyFromName(typeName);
            }

            // Extract type references in field/variable declarations
            Pattern fieldPattern = Pattern.compile(
                    "(?:private|protected|public|\\s)\\s+([A-Z][A-Za-z0-9_<>]*)\\s+\\w+");
            Matcher fieldMatcher = fieldPattern.matcher(source);
            while (fieldMatcher.find()) {
                String typeName = fieldMatcher.group(1);
                // Skip generic parameter parts
                if (!typeName.contains("<")) {
                    visitor.addDependencyFromName(typeName);
                }
            }

            System.out.println("Extracted dependencies from source for: " + classPath.getFileName());

        } catch (Exception e) {
            System.err.println("Error in source-based extraction: " + e.getMessage());
        }
    }

    /**
     * Extracts imports directly from the CompilationUnit without using bindings
     */
    private void extractImportsDirectly(CompilationUnit cu, TypeDependencyVisitor visitor) {
        try {
            // Extract imports directly from the AST
            List imports = cu.imports();
            if (imports != null) {
                for (Object o : imports) {
                    if (o instanceof ImportDeclaration) {
                        ImportDeclaration importDecl = (ImportDeclaration) o;
                        String importName = importDecl.getName().getFullyQualifiedName();
                        if (importDecl.isOnDemand()) {
                            // Add the package to on-demand imports
                            visitor.addOnDemandImport(importName);
                        } else {
                            // Add the specific import
                            String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
                            visitor.addImport(simpleName, importName);

                            // Also add this as a dependency
                            visitor.addDependencyIfNotExists(new Dependency(importName));
                        }
                    }
                }
            }

            // Try to extract the package name
            PackageDeclaration packageDecl = cu.getPackage();
            if (packageDecl != null) {
                String packageName = packageDecl.getName().getFullyQualifiedName();
                visitor.addDependencyIfNotExists(new Dependency(packageName));
            }
        } catch (Exception e) {
            System.err.println("Error extracting imports directly: " + e.getMessage());
        }
    }

    /**
     * ASTVisitor implementation to collect type references
     */
    @Getter
    private static class TypeDependencyVisitor extends ASTVisitor {
        private final List<Dependency> dependencies = new ArrayList<>();
        private final Map<String, String> importMap = new HashMap<>();
        private final Set<String> onDemandImports = new HashSet<>();

        /**
         * Adds an import to the import map
         */
        public void addImport(String simpleName, String fullName) {
            if (simpleName != null && fullName != null && !simpleName.isEmpty() && !fullName.isEmpty()) {
                importMap.put(simpleName, fullName);
            }
        }

        /**
         * Adds an on-demand import to the set
         */
        public void addOnDemandImport(String packageName) {
            if (packageName != null && !packageName.isEmpty()) {
                onDemandImports.add(packageName);
            }
        }

        @Override
        public boolean visit(ImportDeclaration node) {
            try {
                if (node.isOnDemand()) {
                    // Wildcard import (e.g., java.util.*)
                    String packageName = node.getName().getFullyQualifiedName();
                    onDemandImports.add(packageName);
                    log.debug("Added wildcard import: {}", packageName);
                } else {
                    // Explicit import
                    String fullName = node.getName().getFullyQualifiedName();
                    String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
                    importMap.put(simpleName, fullName);
                    log.debug("Added explicit import: {} -> {}", simpleName, fullName);
                }
            } catch (Exception e) {
                System.err.println("Error processing import: " + e.getMessage());
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            try {
                // Handle superclass
                if (node.getSuperclassType() != null) {
                    try {
                        ITypeBinding binding = node.getSuperclassType().resolveBinding();
                        if (binding != null) {
                            addDependencyWithImportResolution(binding);
                        } else {
                            // Fallback: try to get name from AST
                            addTypeFromAST(node.getSuperclassType());
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving superclass: " + e.getMessage());
                        // Try to extract information without bindings
                        addTypeFromAST(node.getSuperclassType());
                    }
                }

                // Handle implemented interfaces
                for (Object o : node.superInterfaceTypes()) {
                    try {
                        Type interfaceType = (Type) o;
                        ITypeBinding binding = interfaceType.resolveBinding();
                        if (binding != null) {
                            addDependencyWithImportResolution(binding);
                        } else {
                            // Fallback: try to get name from AST
                            addTypeFromAST(interfaceType);
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving interface: " + e.getMessage());
                        // Try to extract using AST node
                        if (o instanceof Type) {
                            addTypeFromAST((Type) o);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing type declaration: " + e.getMessage());
            }

            return super.visit(node);
        }

        /**
         * Visit a Record declaration (Java 16+)
         */
        @Override
        public boolean visit(RecordDeclaration node) {
            try {
                // Handle implemented interfaces
                for (Object o : node.superInterfaceTypes()) {
                    try {
                        Type interfaceType = (Type) o;
                        ITypeBinding binding = interfaceType.resolveBinding();
                        if (binding != null) {
                            addDependencyWithImportResolution(binding);
                        } else {
                            addTypeFromAST(interfaceType);
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving record interface: " + e.getMessage());
                        if (o instanceof Type) {
                            addTypeFromAST((Type) o);
                        }
                    }
                }

                // Record parameter types
                for (Object o : node.typeParameters()) {
                    try {
                        SingleVariableDeclaration param = (SingleVariableDeclaration) o;
                        ITypeBinding paramType = param.getType().resolveBinding();
                        if (paramType != null) {
                            addTypeAndGenericsWithImportResolution(paramType);
                        } else {
                            addTypeFromAST(param.getType());
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving record parameter: " + e.getMessage());
                    }
                }

                // Handle record components
                for (Object o : node.recordComponents()) {
                    try {
                        SingleVariableDeclaration component = (SingleVariableDeclaration) o;
                        ITypeBinding binding = component.getType().resolveBinding();
                        if (binding != null) {
                            addTypeAndGenericsWithImportResolution(binding);
                        } else {
                            addTypeFromAST(component.getType());
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving record component: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing record declaration: " + e.getMessage());
            }

            return super.visit(node);
        }

        /**
         * Visit an Annotation declaration
         */
        @Override
        public boolean visit(AnnotationTypeDeclaration node) {
            try {
                // Process annotation type members
                for (Object o : node.bodyDeclarations()) {
                    try {
                        if (o instanceof AnnotationTypeMemberDeclaration member) {
                            ITypeBinding binding = member.getType().resolveBinding();
                            if (binding != null) {
                                addTypeAndGenericsWithImportResolution(binding);
                            } else {
                                addTypeFromAST(member.getType());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving annotation member: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing annotation declaration: " + e.getMessage());
            }

            return super.visit(node);
        }

        /**
         * Visit a SingleMemberAnnotation declaration
         */
        @Override
        public boolean visit(SingleMemberAnnotation node) {
            try {
                ITypeBinding binding = node.resolveTypeBinding();
                if (binding != null) {
                    addTypeAndGenericsWithImportResolution(binding);
                } else {
                    // Fallback: use the annotation's name
                    String name = node.getTypeName().getFullyQualifiedName();
                    addDependencyFromName(name);
                }
            } catch (Exception e) {
                System.err.println("Error processing single member annotation: " + e.getMessage());
                // Fallback: try to extract the name directly
                try {
                    String name = node.getTypeName().getFullyQualifiedName();
                    addDependencyFromName(name);
                } catch (Exception ex) {
                    // Ignore if even this fails
                }
            }

            return super.visit(node);
        }

        /**
         * Visit a MarkerAnnotation declaration
         */
        @Override
        public boolean visit(MarkerAnnotation node) {
            try {
                ITypeBinding binding = node.resolveTypeBinding();
                if (binding != null) {
                    addTypeAndGenericsWithImportResolution(binding);
                } else {
                    // Fallback: use the annotation's name
                    String name = node.getTypeName().getFullyQualifiedName();
                    addDependencyFromName(name);
                }
            } catch (Exception e) {
                System.err.println("Error processing marker annotation: " + e.getMessage());
                // Fallback: try to extract the name directly
                try {
                    String name = node.getTypeName().getFullyQualifiedName();
                    addDependencyFromName(name);
                } catch (Exception ex) {
                    // Ignore if even this fails
                }
            }

            return super.visit(node);
        }

        /**
         * Visit a NormalAnnotation declaration
         */
        @Override
        public boolean visit(NormalAnnotation node) {
            try {
                ITypeBinding binding = node.resolveTypeBinding();
                if (binding != null) {
                    addTypeAndGenericsWithImportResolution(binding);
                } else {
                    // Fallback: use the annotation's name
                    String name = node.getTypeName().getFullyQualifiedName();
                    addDependencyFromName(name);
                }
            } catch (Exception e) {
                System.err.println("Error processing normal annotation: " + e.getMessage());
                // Fallback: try to extract the name directly
                try {
                    String name = node.getTypeName().getFullyQualifiedName();
                    addDependencyFromName(name);
                } catch (Exception ex) {
                    // Ignore if even this fails
                }
            }

            return super.visit(node);
        }

        @Override
        public boolean visit(FieldDeclaration node) {
            try {
                ITypeBinding binding = node.getType().resolveBinding();
                if (binding != null) {
                    addTypeAndGenericsWithImportResolution(binding);
                } else {
                    addTypeFromAST(node.getType());
                }
            } catch (Exception e) {
                System.err.println("Error processing field: " + e.getMessage());
                try {
                    addTypeFromAST(node.getType());
                } catch (Exception ex) {
                    // Ignore if even this fails
                }
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            try {
                // Return type
                if (node.getReturnType2() != null) {
                    try {
                        ITypeBinding returnType = node.getReturnType2().resolveBinding();
                        if (returnType != null && !returnType.isPrimitive()) {
                            addTypeAndGenericsWithImportResolution(returnType);
                        } else {
                            addTypeFromAST(node.getReturnType2());
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving return type: " + e.getMessage());
                        addTypeFromAST(node.getReturnType2());
                    }
                }

                // Parameter types
                for (Object o : node.parameters()) {
                    try {
                        SingleVariableDeclaration param = (SingleVariableDeclaration) o;
                        ITypeBinding paramType = param.getType().resolveBinding();
                        if (paramType != null) {
                            addTypeAndGenericsWithImportResolution(paramType);
                        } else {
                            addTypeFromAST(param.getType());
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving parameter type: " + e.getMessage());
                        if (o instanceof SingleVariableDeclaration) {
                            addTypeFromAST(((SingleVariableDeclaration) o).getType());
                        }
                    }
                }

                // Thrown exceptions
                for (Object o : node.thrownExceptionTypes()) {
                    try {
                        Type exceptionType = (Type) o;
                        ITypeBinding binding = exceptionType.resolveBinding();
                        if (binding != null) {
                            addTypeAndGenericsWithImportResolution(binding);
                        } else {
                            addTypeFromAST(exceptionType);
                        }
                    } catch (Exception e) {
                        System.err.println("Error resolving exception type: " + e.getMessage());
                        if (o instanceof Type) {
                            addTypeFromAST((Type) o);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing method declaration: " + e.getMessage());
            }

            return super.visit(node);
        }

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            try {
                ITypeBinding binding = node.getType().resolveBinding();
                if (binding != null) {
                    addTypeAndGenericsWithImportResolution(binding);
                } else {
                    addTypeFromAST(node.getType());
                }
            } catch (Exception e) {
                System.err.println("Error processing variable declaration: " + e.getMessage());
                try {
                    addTypeFromAST(node.getType());
                } catch (Exception ex) {
                    // Ignore if even this fails
                }
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(ClassInstanceCreation node) {
            try {
                ITypeBinding binding = node.getType().resolveBinding();
                if (binding != null) {
                    addTypeAndGenericsWithImportResolution(binding);
                } else {
                    addTypeFromAST(node.getType());
                }
            } catch (Exception e) {
                System.err.println("Error processing class instance creation: " + e.getMessage());
                try {
                    addTypeFromAST(node.getType());
                } catch (Exception ex) {
                    // Ignore if even this fails
                }
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodInvocation node) {
            try {
                // If there's a target expression, get its type as a dependency
                if (node.getExpression() != null) {
                    ITypeBinding binding = node.getExpression().resolveTypeBinding();
                    if (binding != null) {
                        addTypeAndGenericsWithImportResolution(binding);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing method invocation: " + e.getMessage());
            }
            return super.visit(node);
        }

        /**
         * Helper method to extract type information from AST nodes when binding resolution fails
         */
        private void addTypeFromAST(Type type) {
            if (type == null) return;

            try {
                if (type.isSimpleType()) {
                    SimpleType simpleType = (SimpleType) type;
                    String name = simpleType.getName().getFullyQualifiedName();
                    addDependencyFromName(name);
                } else if (type.isQualifiedType()) {
                    QualifiedType qualifiedType = (QualifiedType) type;
                    String name = qualifiedType.getName().getIdentifier();
                    Type qualifier = qualifiedType.getQualifier();

                    if (qualifier != null && qualifier.isSimpleType()) {
                        String qualifierName = ((SimpleType) qualifier).getName().getFullyQualifiedName();
                        name = qualifierName + "." + name;
                    }

                    addDependencyFromName(name);
                } else if (type.isParameterizedType()) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    addTypeFromAST(parameterizedType.getType());

                    // Also add type arguments
                    for (Object o : parameterizedType.typeArguments()) {
                        if (o instanceof Type) {
                            addTypeFromAST((Type) o);
                        }
                    }
                } else if (type.isArrayType()) {
                    ArrayType arrayType = (ArrayType) type;
                    addTypeFromAST(arrayType.getElementType());
                } else if (type.isNameQualifiedType()) {
                    NameQualifiedType nameQualifiedType = (NameQualifiedType) type;
                    String name = nameQualifiedType.getName().getIdentifier();
                    String qualifier = nameQualifiedType.getQualifier().getFullyQualifiedName();

                    addDependencyFromName(qualifier + "." + name);
                }
            } catch (Exception e) {
                System.err.println("Error extracting type from AST: " + e.getMessage());
            }
        }

        /**
         * Helper method to add a dependency from a simple name
         */
        public void addDependencyFromName(String name) {
            if (name == null || name.isEmpty()) return;

            // Skip types we don't want to include
            if (shouldSkipType(name)) {
                return;
            }

            // First check if it's already a qualified name
            if (name.contains(".")) {
                // If it appears to be a suspicious resolution, try to validate it
                if (isPotentiallyIncorrectPackageResolution(name, name.substring(name.lastIndexOf('.') + 1))) {
                    // Extract the simple name
                    String simpleName = name.substring(name.lastIndexOf('.') + 1);

                    // Try to resolve with other methods
                    // First check imports
                    if (importMap.containsKey(simpleName)) {
                        addDependencyIfNotExists(new Dependency(importMap.get(simpleName)));
                        return;
                    }

                    // Try standard packages with Class.forName
                    if (isTypeInPackage("java.lang", simpleName)) {
                        // Skip java.lang types
                        return;
                    }

                    if (isTypeInPackage("java.time", simpleName)) {
                        addDependencyIfNotExists(new Dependency("java.time." + simpleName));
                        return;
                    }

                    if (isTypeInPackage("java.util", simpleName)) {
                        addDependencyIfNotExists(new Dependency("java.util." + simpleName));
                        return;
                    }

                    // Fall back to simple name
                    addDependencyIfNotExists(new Dependency(simpleName));
                } else {
                    // Normal qualified name that looks valid
                    addDependencyIfNotExists(new Dependency(name));
                }
                return;
            }

            // Check explicit imports - most reliable method
            if (importMap.containsKey(name)) {
                addDependencyIfNotExists(new Dependency(importMap.get(name)));
                return;
            }

            // Check wildcard imports - try to resolve against each one
            String resolvedType = resolveTypeFromWildcardImports(name);
            if (resolvedType != null) {
                addDependencyIfNotExists(new Dependency(resolvedType));
                return;
            }

            // Check for java.lang.* implicit imports using Class.forName
            if (isTypeInPackage("java.lang", name)) {
                // Don't add java.lang types as dependencies - they're implicit
                return;
            }

            // Check for java.time.* types using Class.forName
            if (isTypeInPackage("java.time", name)) {
                addDependencyIfNotExists(new Dependency("java.time." + name));
                return;
            }

            // Check for java.util.* types using Class.forName
            if (isTypeInPackage("java.util", name)) {
                addDependencyIfNotExists(new Dependency("java.util." + name));
                return;
            }

            // Last resort - use the name as is if it's not a type to skip
            if (!shouldSkipType(name)) {
                addDependencyIfNotExists(new Dependency(name));
            }
        }

        /**
         * Helper method to add a dependency while resolving imports with priority for external libraries
         */
        private void addDependencyWithImportResolution(ITypeBinding binding) {
            try {
                // Skip primitive types
                if (binding.isPrimitive()) {
                    return;
                }

                String qualifiedName = binding.getQualifiedName();
                String simpleName = binding.getName();

                // Skip primitive wrapper types, common Java types, and other types we don't want to track
                if (shouldSkipType(simpleName)) {
                    return;
                }

                log.debug("Resolving binding for type: {} (qualified: {})", simpleName, qualifiedName);

                // PRIORITY 1: Check explicit imports first - most reliable
                if (importMap.containsKey(simpleName)) {
                    String explicitImport = importMap.get(simpleName);
                    log.debug("Found explicit import for {}: {}", simpleName, explicitImport);
                    addDependencyIfNotExists(new Dependency(explicitImport));
                    return;
                }

                // PRIORITY 2: Check for common external library types
                String externalLibraryType = resolveToExternalLibrary(simpleName);
                if (externalLibraryType != null) {
                    log.debug("Resolved {} to external library: {}", simpleName, externalLibraryType);
                    addDependencyIfNotExists(new Dependency(externalLibraryType));
                    return;
                }

                // PRIORITY 3: Use binding resolution if it points to external library
                if (qualifiedName != null && qualifiedName.contains(".")) {
                    // Accept if it's clearly an external library (not project package)
                    if (isExternalLibraryType(qualifiedName)) {
                        log.debug("Accepted external library binding for {}: {}", simpleName, qualifiedName);
                        addDependencyIfNotExists(new Dependency(qualifiedName));
                        return;
                    } else if (isPotentiallyIncorrectPackageResolution(qualifiedName, simpleName)) {
                        log.debug("Rejected suspicious project binding for {}: {}", simpleName, qualifiedName);
                        // Continue to other resolution methods
                    } else {
                        // Accept other qualified names that don't look suspicious
                        addDependencyIfNotExists(new Dependency(qualifiedName));
                        return;
                    }
                }

                // PRIORITY 4: Check wildcard imports
                String wildcardResolved = resolveTypeFromWildcardImports(simpleName);
                if (wildcardResolved != null && isExternalLibraryType(wildcardResolved)) {
                    log.debug("Resolved {} via wildcard to external library: {}", simpleName, wildcardResolved);
                    addDependencyIfNotExists(new Dependency(wildcardResolved));
                    return;
                }

                // PRIORITY 5: Last resort - use simple name for project types only
                log.debug("Using simple name as last resort for: {}", simpleName);
                if (!shouldSkipType(simpleName)) {
                    addDependencyIfNotExists(new Dependency(simpleName));
                }
            } catch (Exception e) {
                log.error("Error resolving dependency for {}: {}", binding.getName(), e.getMessage());
            }
        }

        /**
         * Checks if the type should be skipped (primitives, wrappers, etc.)
         */
        private boolean shouldSkipType(String simpleName) {
            // Skip primitive types
            if (simpleName.equals("boolean") || simpleName.equals("byte") ||
                    simpleName.equals("char") || simpleName.equals("double") ||
                    simpleName.equals("float") || simpleName.equals("int") ||
                    simpleName.equals("long") || simpleName.equals("short") ||
                    simpleName.equals("void")) {
                return true;
            }

            // Skip wrapper types
            if (simpleName.equals("Boolean") || simpleName.equals("Byte") ||
                    simpleName.equals("Character") || simpleName.equals("Double") ||
                    simpleName.equals("Float") || simpleName.equals("Integer") ||
                    simpleName.equals("Long") || simpleName.equals("Short") ||
                    simpleName.equals("Void") || simpleName.equals("String")) {
                return true;
            }

            // Skip other common types we don't want to track
            return simpleName.equals("Object") || simpleName.equals("Class") ||
                    simpleName.equals("Enum") || simpleName.equals("Override") ||
                    simpleName.equals("SuppressWarnings") || simpleName.equals("Deprecated") ||
                    simpleName.equals("FunctionalInterface");
        }

        /**
         * Checks if a type is likely to have been incorrectly resolved to the wrong package
         */
        private boolean isPotentiallyIncorrectPackageResolution(String qualifiedName, String simpleName) {
            // Case: Type is being resolved to a project-specific package when it might be a standard type
            if (qualifiedName.contains(".domain.") ||
                    qualifiedName.contains(".dto.") ||
                    qualifiedName.contains(".model.") ||
                    qualifiedName.contains(".entity.") ||
                    qualifiedName.contains(".controller.") ||
                    qualifiedName.contains(".service.")) {

                // Try to verify if it exists in java.* packages using Class.forName
                return isTypeInPackage("java.lang", simpleName) ||
                        isTypeInPackage("java.time", simpleName) ||
                        isTypeInPackage("java.util", simpleName);
            }

            return false;
        }

        /**
         * Resolves a type name against wildcard imports, returning the fully qualified name if found
         */
        private String resolveTypeFromWildcardImports(String typeName) {
            for (String packageName : onDemandImports) {
                // Skip java.lang as it's implicit
                if ("java.lang".equals(packageName)) {
                    continue;
                }
                
                try {
                    // Try to see if this type exists in this package
                    String fullyQualifiedName = packageName + "." + typeName;
                    
                    // For project-specific packages, assume the type exists if the package path makes sense
                    if (packageName.contains(".domain.") || packageName.contains(".dto.") || 
                        packageName.contains(".entity.") || packageName.contains(".model.")) {
                        
                        // Special handling for Attendee.java case: Event should resolve to tcc.com.pass_in.domain.event.Event
                        if ("Event".equals(typeName) && packageName.contains(".domain.event")) {
                            log.debug("Resolved {} to {} via project wildcard import", typeName, fullyQualifiedName);
                            return fullyQualifiedName;
                        }
                        
                        // For other project types, also assume they exist
                        if (isLikelyProjectType(typeName)) {
                            log.debug("Resolved {} to {} via project wildcard import", typeName, fullyQualifiedName);
                            return fullyQualifiedName;
                        }
                    }
                    
                    // For standard library packages, verify with Class.forName
                    if (isTypeInPackage(packageName, typeName)) {
                        log.debug("Resolved {} to {} via verified wildcard import", typeName, fullyQualifiedName);
                        return fullyQualifiedName;
                    }
                    
                } catch (Exception e) {
                    // Continue to next package
                }
            }
            return null; // Type not found in any wildcard import
        }
        
        /**
         * Resolves a simple type name to a known external library type
         */
        private String resolveToExternalLibrary(String simpleName) {
            // Jakarta Persistence API
            if ("Entity".equals(simpleName)) return "jakarta.persistence.Entity";
            if ("Table".equals(simpleName)) return "jakarta.persistence.Table";
            if ("Id".equals(simpleName)) return "jakarta.persistence.Id";
            if ("Column".equals(simpleName)) return "jakarta.persistence.Column";
            if ("GeneratedValue".equals(simpleName)) return "jakarta.persistence.GeneratedValue";
            if ("ManyToOne".equals(simpleName)) return "jakarta.persistence.ManyToOne";
            if ("OneToMany".equals(simpleName)) return "jakarta.persistence.OneToMany";
            if ("JoinColumn".equals(simpleName)) return "jakarta.persistence.JoinColumn";
            if ("GenerationType".equals(simpleName)) return "jakarta.persistence.GenerationType";
            
            // Lombok
            if ("Getter".equals(simpleName)) return "lombok.Getter";
            if ("Setter".equals(simpleName)) return "lombok.Setter";
            if ("NoArgsConstructor".equals(simpleName)) return "lombok.NoArgsConstructor";
            if ("AllArgsConstructor".equals(simpleName)) return "lombok.AllArgsConstructor";
            if ("Data".equals(simpleName)) return "lombok.Data";
            if ("Builder".equals(simpleName)) return "lombok.Builder";
            if ("ToString".equals(simpleName)) return "lombok.ToString";
            if ("EqualsAndHashCode".equals(simpleName)) return "lombok.EqualsAndHashCode";
            
            // Spring Framework
            if ("ControllerAdvice".equals(simpleName)) return "org.springframework.web.bind.annotation.ControllerAdvice";
            if ("RestController".equals(simpleName)) return "org.springframework.web.bind.annotation.RestController";
            if ("RequestMapping".equals(simpleName)) return "org.springframework.web.bind.annotation.RequestMapping";
            if ("GetMapping".equals(simpleName)) return "org.springframework.web.bind.annotation.GetMapping";
            if ("PostMapping".equals(simpleName)) return "org.springframework.web.bind.annotation.PostMapping";
            if ("PutMapping".equals(simpleName)) return "org.springframework.web.bind.annotation.PutMapping";
            if ("DeleteMapping".equals(simpleName)) return "org.springframework.web.bind.annotation.DeleteMapping";
            if ("PathVariable".equals(simpleName)) return "org.springframework.web.bind.annotation.PathVariable";
            if ("RequestBody".equals(simpleName)) return "org.springframework.web.bind.annotation.RequestBody";
            if ("Service".equals(simpleName)) return "org.springframework.stereotype.Service";
            if ("Repository".equals(simpleName)) return "org.springframework.stereotype.Repository";
            if ("Component".equals(simpleName)) return "org.springframework.stereotype.Component";
            if ("Autowired".equals(simpleName)) return "org.springframework.beans.factory.annotation.Autowired";
            if ("ExceptionHandler".equals(simpleName)) return "org.springframework.web.bind.annotation.ExceptionHandler";
            if ("ResponseStatus".equals(simpleName)) return "org.springframework.web.bind.annotation.ResponseStatus";
            
            // Java Time API
            if ("LocalDateTime".equals(simpleName)) return "java.time.LocalDateTime";
            if ("LocalDate".equals(simpleName)) return "java.time.LocalDate";
            if ("LocalTime".equals(simpleName)) return "java.time.LocalTime";
            if ("ZonedDateTime".equals(simpleName)) return "java.time.ZonedDateTime";
            if ("Instant".equals(simpleName)) return "java.time.Instant";
            
            return null; // Not a known external library type
        }
        
        /**
         * Checks if a qualified name represents an external library type
         */
        private boolean isExternalLibraryType(String qualifiedName) {
            if (qualifiedName == null) return false;
            
            // External library prefixes
            return qualifiedName.startsWith("jakarta.") ||
                   qualifiedName.startsWith("lombok.") ||
                   qualifiedName.startsWith("org.springframework.") ||
                   qualifiedName.startsWith("org.apache.") ||
                   qualifiedName.startsWith("com.fasterxml.") ||
                   qualifiedName.startsWith("java.") ||
                   qualifiedName.startsWith("javax.") ||
                   qualifiedName.startsWith("org.slf4j.") ||
                   qualifiedName.startsWith("org.hibernate.") ||
                   qualifiedName.startsWith("com.google.") ||
                   // Add more external library prefixes as needed
                   (!qualifiedName.contains(".domain.") && 
                    !qualifiedName.contains(".dto.") && 
                    !qualifiedName.contains(".model.") && 
                    !qualifiedName.contains(".entity.") && 
                    !qualifiedName.contains(".controller.") && 
                    !qualifiedName.contains(".service.") &&
                    !qualifiedName.contains(".repository.") &&
                    !qualifiedName.startsWith("tcc.com."));
        }

        /**
         * Checks if a type name is likely to be a project-specific type
         */
        private boolean isLikelyProjectType(String typeName) {
            // Project types typically start with capital letter and are not primitives/wrappers
            return typeName.length() > 0 && 
                   Character.isUpperCase(typeName.charAt(0)) && 
                   !shouldSkipType(typeName) &&
                   !isTypeInPackage("java.lang", typeName) &&
                   !isTypeInPackage("java.util", typeName) &&
                   !isTypeInPackage("java.time", typeName);
        }

        /**
         * Checks if a type exists in a given package using Class.forName
         */
        private boolean isTypeInPackage(String packageName, String typeName) {
            try {
                Class.forName(packageName + "." + typeName);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        /**
         * Helper method to check if a dependency already exists and add it if not
         */
        public void addDependencyIfNotExists(Dependency dependency) {
            try {
                if (dependency.getName() != null && !dependency.getName().isEmpty() &&
                        dependency.dependencyDoesNotExist(dependencies)) {
                    dependencies.add(dependency);
                }
            } catch (Exception e) {
                System.err.println("Error adding dependency: " + e.getMessage());
            }
        }

        /**
         * Helper method to add a type and its generic type arguments with import resolution
         */
        private void addTypeAndGenericsWithImportResolution(ITypeBinding binding) {
            try {
                // Add the base type
                addDependencyWithImportResolution(binding);

                // Add generic type arguments if any
                if (binding.isParameterizedType()) {
                    for (ITypeBinding typeArg : binding.getTypeArguments()) {
                        if (typeArg != null && !typeArg.isPrimitive()) {
                            addDependencyWithImportResolution(typeArg);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error resolving generic type: " + e.getMessage());
            }
        }
    }
}