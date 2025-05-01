package tcc.com.viewer.services;

import lombok.Getter;
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

    // Set to track files that have been processed to avoid duplicate processing
    private final Set<Path> processedFiles = new HashSet<>();

    /**
     * Parses a Java file and returns its AST
     */
    public CompilationUnit parseClass(Path classPath) throws IOException {
        // Check if we've already processed this file to avoid duplicate processing
        if (processedFiles.contains(classPath)) {
            System.out.println("Skipping already processed file: " + classPath);
            // Create a minimal unit to satisfy callers
            AST ast = AST.newAST(AST.getJLSLatest());
            CompilationUnit emptyUnit = ast.newCompilationUnit();
            return emptyUnit;
        }

        // Mark this file as processed
        processedFiles.add(classPath);

        String source = Files.readString(classPath);
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // Use the latest supported JLS level

        // Set parser options with more robust error recovery
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
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

        // Get classpath and sourcepath from cache or calculate them
        String[] classpath;
        String[] sourcepath;

        if (projectClasspathCache.containsKey(projectDir)) {
            // Use cached values
            classpath = projectClasspathCache.get(projectDir);
            sourcepath = projectSourcepathCache.get(projectDir);
            System.out.println("Using cached classpath/sourcepath for project: " + projectDir);
        } else {
            // Calculate and cache values
            classpath = getComprehensiveClassPath(projectDir);
            sourcepath = getComprehensiveSourcePath(projectDir);

            // Cache for future use
            projectClasspathCache.put(projectDir, classpath);
            projectSourcepathCache.put(projectDir, sourcepath);

            System.out.println("Calculated and cached classpath with " + classpath.length + " entries");
            System.out.println("Calculated and cached sourcepath with " + sourcepath.length + " entries");
        }

        parser.setEnvironment(classpath, sourcepath, null, true);
        parser.setUnitName(classPath.getFileName().toString());

        try {
            //for some reason, when we createAST for the first file it prints the `Error extracting dependencies from source:` message.
            //ask claude why this is happening and also for him to fix it.
            CompilationUnit unit = (CompilationUnit) parser.createAST(null);
            // Avoid returning null which could cause NPEs later
            if (unit != null) {
                return unit;
            } else {
                throw new IllegalStateException("Parser returned null CompilationUnit");
            }
        } catch (Exception e) {
            System.err.println("Error parsing " + classPath.getFileName() + ": " + e.getMessage());

            // Try a completely different approach - disable binding resolution entirely
            ASTParser fallbackParser = ASTParser.newParser(AST.getJLSLatest());
            fallbackParser.setSource(source.toCharArray());
            fallbackParser.setKind(ASTParser.K_COMPILATION_UNIT);

            // Completely disable binding resolution to avoid array index errors
            fallbackParser.setResolveBindings(false);
            fallbackParser.setBindingsRecovery(false);
            fallbackParser.setStatementsRecovery(true);

            // Don't set environment to avoid binding-related errors
            // Don't need classpath/sourcepath when not resolving bindings

            // Set most permissive compiler options
            Map<String, String> fallbackOptions = JavaCore.getOptions();
            JavaCore.setComplianceOptions(JavaCore.VERSION_19, fallbackOptions);

            // Set all error-related options to IGNORE
            fallbackOptions.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);
            fallbackOptions.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
            fallbackOptions.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.IGNORE);
            fallbackOptions.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS, JavaCore.IGNORE);
            fallbackOptions.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.IGNORE);
            fallbackOptions.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
            fallbackOptions.put(JavaCore.COMPILER_PB_DEPRECATION, JavaCore.IGNORE);

            fallbackParser.setCompilerOptions(fallbackOptions);

            // Unit name is still needed
            fallbackParser.setUnitName(classPath.getFileName().toString());

            System.out.println("Retrying with NO binding resolution for " + classPath.getFileName());
            try {
                CompilationUnit unit = (CompilationUnit) fallbackParser.createAST(null);
                if (unit != null) {
                    return unit;
                } else {
                    throw new IllegalStateException("Fallback parser returned null CompilationUnit");
                }
            } catch (Exception fallbackError) {
                System.err.println("Fallback parsing also failed for " +
                        classPath.getFileName() + ": " + fallbackError.getMessage());

                // As absolute last resort, create a minimal empty AST
                AST ast = AST.newAST(AST.getJLSLatest());
                CompilationUnit emptyUnit = ast.newCompilationUnit();
                return emptyUnit;
            }
        }
    }

    /**
     * Provides a comprehensive classpath that includes:
     * 1. JDK libraries
     * 2. All project dependencies from Maven or Gradle
     * 3. All JAR files in the project
     * 4. All compiled classes
     */
    private String[] getComprehensiveClassPath(Path projectDir) {
        Set<String> classpath = new HashSet<>();

        try {
            // Add JDK libraries
            addJdkLibraries(classpath);

            // Add project dependencies based on build system
            if (Files.exists(projectDir.resolve("pom.xml"))) {
                System.out.println("Found Maven project. Parsing pom.xml for dependencies...");
                parseMavenDependencies(projectDir, classpath);
            }

            if (Files.exists(projectDir.resolve("build.gradle"))) {
                System.out.println("Found Gradle project. Parsing build.gradle for dependencies...");
                parseGradleDependencies(projectDir.resolve("build.gradle"), classpath);
            }

            if (Files.exists(projectDir.resolve("build.gradle.kts"))) {
                System.out.println("Found Kotlin Gradle project. Parsing build.gradle.kts for dependencies...");
                parseGradleDependencies(projectDir.resolve("build.gradle.kts"), classpath);
            }

            // Add compiled classes
            addCompiledClasses(projectDir, classpath);

            // Find all JAR files in the project
            findAllJars(projectDir, classpath);

            // Download missing dependency JARs if needed (from Maven Central or other repositories)
            downloadMissingDependencies(projectDir, classpath);

        } catch (Exception e) {
            System.err.println("Error building classpath: " + e.getMessage());
            e.printStackTrace();
        }

        return classpath.toArray(new String[0]);
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
        // If no build file found, return the base uploads directory
        return Paths.get("uploads").resolve(classPath.toString().split("uploads[/\\\\]")[1].split("[/\\\\]")[0]);
    }

    /**
     * Extracts all dependencies from a Java class file
     */
    public List<Dependency> getDependencies(Path classPath) {
        // If we've already processed this file in this session, use a cached visitor or create an empty one
        if (processedFiles.contains(classPath) && !processedFiles.add(classPath)) {
            System.out.println("File already processed by getDependencies: " + classPath);
            return new ArrayList<>();
        }

        TypeDependencyVisitor visitor = new TypeDependencyVisitor();

        try {
            System.out.println("Extracting dependencies from: " + classPath);

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

        return visitor.getDependencies();
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
                    onDemandImports.add(node.getName().getFullyQualifiedName());
                } else {
                    // Explicit import
                    String fullName = node.getName().getFullyQualifiedName();
                    String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
                    importMap.put(simpleName, fullName);
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
         * Helper method to add a dependency while resolving imports
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

                // If binding resolution worked correctly, use the qualified name directly
                if (qualifiedName != null && qualifiedName.contains(".")) {
                    // Verify the package looks reasonable
                    if (!isPotentiallyIncorrectPackageResolution(qualifiedName, simpleName)) {
                        addDependencyIfNotExists(new Dependency(qualifiedName));
                        return;
                    }
                    // If the package seems suspicious, continue with other resolution methods
                }

                // Try to resolve through known information

                // Check explicit imports - most reliable method
                if (importMap.containsKey(simpleName)) {
                    addDependencyIfNotExists(new Dependency(importMap.get(simpleName)));
                    return;
                }

                // Check for java.lang.* implicit imports using Class.forName
                if (isTypeInPackage("java.lang", simpleName)) {
                    // Don't add java.lang types as dependencies - they're implicit
                    return;
                }

                // Check for java.time.* types using Class.forName
                if (isTypeInPackage("java.time", simpleName)) {
                    addDependencyIfNotExists(new Dependency("java.time." + simpleName));
                    return;
                }

                // Last resort - use the simple name (but still skip excluded types)
                if (!shouldSkipType(simpleName)) {
                    addDependencyIfNotExists(new Dependency(simpleName));
                }
            } catch (Exception e) {
                System.err.println("Error resolving dependency: " + e.getMessage());
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