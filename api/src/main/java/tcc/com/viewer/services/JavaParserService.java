package tcc.com.viewer.services;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.util.PackageNameExtractor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JavaParser-based service for extracting dependencies from Java projects.
 * This service replaces the Eclipse JDT implementation with JavaParser for better performance
 * and more reliable dependency resolution.
 */
@Slf4j
@Service
public class JavaParserService {

    private static final int BATCH_SIZE = 100;
    private static final Pattern GRADLE_DEP_PATTERN = Pattern.compile(
            "(implementation|api|compileOnly|runtimeOnly|testImplementation)\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern GRADLE_GROUP_ARTIFACT_VERSION = Pattern.compile(
            "([^:]+):([^:]+):([^:]+)");

    // Cache the TypeSolver per project (project root path as key)
    private final Map<Path, CombinedTypeSolver> typeSolverCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> jarCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final PackageNameExtractor packageNameExtractor = new PackageNameExtractor();

    /**
     * Main method to extract dependencies from a Java class file
     *
     * @param classPath Path to the Java file to analyze
     * @return List of dependencies found in the file
     */
    public List<Dependency> getDependencies(Path classPath) {
        log.info("=== JavaParserService.getDependencies() called for: {}", classPath);

        try {
            // Determine project root
            Path projectRoot = findProjectRoot(classPath);
            if (projectRoot == null) {
                log.warn("Could not determine project root for {}, using parent directory", classPath);
                projectRoot = classPath.getParent();
            }

            // Get or create TypeSolver for this project
            CombinedTypeSolver typeSolver = getOrCreateTypeSolver(projectRoot);

            // Configure JavaParser with the TypeSolver
            ParserConfiguration config = new ParserConfiguration();
            config.setSymbolResolver(new JavaSymbolSolver(typeSolver));
            config.setStoreTokens(false); // Performance optimization
            config.setAttributeComments(false); // Performance optimization

            JavaParser parser = new JavaParser(config);

            // Parse the Java file
            String sourceCode = Files.readString(classPath);
            CompilationUnit cu = parser.parse(sourceCode).getResult().orElse(null);

            if (cu == null) {
                log.error("Failed to parse Java file: {}", classPath);
                return new ArrayList<>();
            }

            // Extract dependencies using our custom visitor
            JavaParserDependencyVisitor visitor = new JavaParserDependencyVisitor(typeSolver);
            cu.accept(visitor, null);

            List<Dependency> dependencies = visitor.getDependencies();

            log.info("Extracted {} dependencies from {}", dependencies.size(), classPath.getFileName());
            return dependencies;

        } catch (Exception e) {
            log.error("Error extracting dependencies from {}: {}", classPath, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Clears the TypeSolver cache between projects to prevent memory leaks
     */
    public void clearCache() {
        typeSolverCache.clear();
        jarCache.clear();
        // Clear JavaParser internal caches
        try {
            JavaParserFacade.clearInstances();
        } catch (Exception e) {
            log.warn("Failed to clear JavaParserFacade instances: {}", e.getMessage());
        }
        log.info("Cleared all caches");
    }

    /**
     * Batch processing for large projects
     *
     * @param classPaths List of Java files to process
     * @return Map of file paths to their dependencies
     */
    public Map<Path, List<Dependency>> processBatch(List<Path> classPaths) {
        return classPaths.parallelStream()
                .collect(Collectors.toMap(
                        path -> path,
                        this::getDependencies,
                        (existing, replacement) -> existing,
                        ConcurrentHashMap::new
                ));
    }

    /**
     * Analyzes an entire project with batch processing
     *
     * @param projectRoot Root directory of the project
     * @return List of all dependencies found in the project
     */
    public List<Dependency> analyzeProject(Path projectRoot) {
        try {
            List<Path> allJavaFiles = findAllJavaFiles(projectRoot);
            List<Dependency> allDependencies = new CopyOnWriteArrayList<>();

            log.info("Found {} Java files to analyze in project {}", allJavaFiles.size(), projectRoot);

            // Process in batches to manage memory
            for (int i = 0; i < allJavaFiles.size(); i += BATCH_SIZE) {
                List<Path> batch = allJavaFiles.subList(
                        i, Math.min(i + BATCH_SIZE, allJavaFiles.size())
                );

                log.info("Processing batch {}/{} ({} files)",
                        (i / BATCH_SIZE) + 1,
                        (allJavaFiles.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                        batch.size());

                Map<Path, List<Dependency>> batchResults = processBatch(batch);
                batchResults.values().forEach(allDependencies::addAll);

                // Suggest garbage collection every 5 batches
                if (i % (BATCH_SIZE * 5) == 0) {
                    System.gc();
                }
            }

            return deduplicateDependencies(allDependencies);

        } catch (IOException e) {
            log.error("Error analyzing project {}: {}", projectRoot, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets or creates a TypeSolver for the given project root
     */
    private CombinedTypeSolver getOrCreateTypeSolver(Path projectRoot) {
        return typeSolverCache.computeIfAbsent(projectRoot, this::buildTypeSolver);
    }

    /**
     * Builds a comprehensive TypeSolver for the project
     */
    private CombinedTypeSolver buildTypeSolver(Path projectRoot) {
        log.info("Building TypeSolver for project: {}", projectRoot);

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();

        // 1. Add ReflectionTypeSolver for JRE classes (highest priority)
        typeSolver.add(new ReflectionTypeSolver(false));

        // 2. Add JavaParserTypeSolver for project sources
        addProjectSources(projectRoot, typeSolver);

        // 3. Add JAR dependencies
        addJarDependencies(projectRoot, typeSolver);

        log.info("TypeSolver built successfully");
        return typeSolver;
    }

    /**
     * Adds project source directories to the TypeSolver
     */
    private void addProjectSources(Path projectRoot, CombinedTypeSolver typeSolver) {
        try {
            // Find all source directories (src/main/java, src/test/java, etc.)
            List<Path> sourceDirs = findSourceDirectories(projectRoot);

            for (Path sourceDir : sourceDirs) {
                if (Files.isDirectory(sourceDir)) {
                    try {
                        typeSolver.add(new JavaParserTypeSolver(sourceDir.toFile()));
                        log.debug("Added source directory to TypeSolver: {}", sourceDir);
                    } catch (Exception e) {
                        log.warn("Failed to add source directory {}: {}", sourceDir, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error adding project sources: {}", e.getMessage());
        }
    }

    /**
     * Adds JAR dependencies to the TypeSolver
     */
    private void addJarDependencies(Path projectRoot, CombinedTypeSolver typeSolver) {
        try {
            List<String> jarPaths = resolveProjectDependencies(projectRoot);

            for (String jarPath : jarPaths) {
                try {
                    if (Files.exists(Paths.get(jarPath)) && jarPath.endsWith(".jar")) {
                        typeSolver.add(new JarTypeSolver(jarPath));
                        log.debug("Added JAR to TypeSolver: {}", jarPath);
                    }
                } catch (Exception e) {
                    log.warn("Failed to add JAR {}: {}", jarPath, e.getMessage());
                }
            }

            log.info("Added {} JAR dependencies to TypeSolver", jarPaths.size());
        } catch (Exception e) {
            log.error("Error adding JAR dependencies: {}", e.getMessage());
        }
    }

    /**
     * Resolves all project dependencies (Maven and Gradle)
     */
    private List<String> resolveProjectDependencies(Path projectRoot) {
        String cacheKey = projectRoot.toString();
        if (jarCache.containsKey(cacheKey)) {
            log.debug("Using cached JAR dependencies for project: {}", projectRoot);
            return jarCache.get(cacheKey);
        }

        List<String> jarPaths = new ArrayList<>();

        // Try Maven first
        Path pomFile = projectRoot.resolve("pom.xml");
        if (Files.exists(pomFile)) {
            jarPaths.addAll(resolveMavenDependencies(pomFile));
            // Add common Spring Boot transitive dependencies that our simple resolver misses
            jarPaths.addAll(resolveSpringBootTransitiveDependencies(pomFile));
        }

        // Try Gradle
        Path gradleBuild = projectRoot.resolve("build.gradle");
        Path gradleBuildKts = projectRoot.resolve("build.gradle.kts");
        if (Files.exists(gradleBuild)) {
            jarPaths.addAll(resolveGradleDependencies(gradleBuild));
        } else if (Files.exists(gradleBuildKts)) {
            jarPaths.addAll(resolveGradleDependencies(gradleBuildKts));
        }

        // Cache the results
        jarCache.put(cacheKey, jarPaths);

        return jarPaths;
    }

    /**
     * Resolves common Spring Boot transitive dependencies that our simple resolver misses
     */
    private List<String> resolveSpringBootTransitiveDependencies(Path pomFile) {
        List<String> jarPaths = new ArrayList<>();

        try {
            String pomContent = Files.readString(pomFile);

            // Check if this is a Spring Boot project with data-jpa
            if (pomContent.contains("spring-boot-starter-data-jpa")) {
                // Add Jakarta persistence API
                String jakartaPersistenceJar = downloadMavenArtifact("jakarta.persistence", "jakarta.persistence-api", "3.1.0");
                if (jakartaPersistenceJar != null) {
                    jarPaths.add(jakartaPersistenceJar);
                    log.debug("Added Jakarta Persistence API JAR: {}", jakartaPersistenceJar);
                }
            }

            // Check if this is a Spring Boot web project 
            if (pomContent.contains("spring-boot-starter-web")) {
                // Add Spring Web annotations
                String springWebJar = downloadMavenArtifact("org.springframework", "spring-web", "6.1.13");
                if (springWebJar != null) {
                    jarPaths.add(springWebJar);
                    log.debug("Added Spring Web JAR: {}", springWebJar);
                }
            }

            log.info("Added {} Spring Boot transitive dependencies", jarPaths.size());

        } catch (Exception e) {
            log.error("Error resolving Spring Boot transitive dependencies: {}", e.getMessage());
        }

        return jarPaths;
    }

    /**
     * Resolves Maven dependencies from pom.xml
     */
    private List<String> resolveMavenDependencies(Path pomFile) {
        List<String> jarPaths = new ArrayList<>();

        try {
            log.info("Resolving Maven dependencies from: {}", pomFile);

            // Parse pom.xml
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile.toFile());
            doc.getDocumentElement().normalize();

            // Get properties for version resolution
            Map<String, String> properties = extractMavenProperties(doc);

            // Extract dependencies
            NodeList dependencyNodes = doc.getElementsByTagName("dependency");

            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node dependencyNode = dependencyNodes.item(i);
                if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element dependency = (Element) dependencyNode;

                    String groupId = getElementText(dependency, "groupId");
                    String artifactId = getElementText(dependency, "artifactId");
                    String version = getElementText(dependency, "version");
                    String scope = getElementText(dependency, "scope");

                    // Skip test dependencies
                    if ("test".equals(scope)) {
                        continue;
                    }

                    // Resolve property placeholders
                    if (version != null && version.startsWith("${")) {
                        String propKey = version.substring(2, version.length() - 1);
                        version = properties.getOrDefault(propKey, version);
                    }

                    if (groupId != null && artifactId != null && version != null) {
                        String jarPath = downloadMavenArtifact(groupId, artifactId, version);
                        if (jarPath != null) {
                            jarPaths.add(jarPath);
                        }
                    }
                }
            }

            // Also check dependency management section
            NodeList depMgmtNodes = doc.getElementsByTagName("dependencyManagement");
            if (depMgmtNodes.getLength() > 0) {
                NodeList managedDeps = ((Element) depMgmtNodes.item(0)).getElementsByTagName("dependency");
                for (int i = 0; i < managedDeps.getLength(); i++) {
                    Node dependencyNode = managedDeps.item(i);
                    if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element dependency = (Element) dependencyNode;

                        String groupId = getElementText(dependency, "groupId");
                        String artifactId = getElementText(dependency, "artifactId");
                        String version = getElementText(dependency, "version");
                        String scope = getElementText(dependency, "scope");

                        // Skip test dependencies
                        if ("test".equals(scope)) {
                            continue;
                        }

                        // Resolve property placeholders
                        if (version != null && version.startsWith("${")) {
                            String propKey = version.substring(2, version.length() - 1);
                            version = properties.getOrDefault(propKey, version);
                        }

                        if (groupId != null && artifactId != null && version != null) {
                            String jarPath = downloadMavenArtifact(groupId, artifactId, version);
                            if (jarPath != null) {
                                jarPaths.add(jarPath);
                            }
                        }
                    }
                }
            }

            log.info("Resolved {} Maven dependencies", jarPaths.size());

        } catch (Exception e) {
            log.error("Error resolving Maven dependencies: {}", e.getMessage(), e);
        }

        return jarPaths;
    }

    /**
     * Resolves Gradle dependencies from build.gradle
     */
    private List<String> resolveGradleDependencies(Path buildFile) {
        List<String> jarPaths = new ArrayList<>();

        try {
            log.info("Resolving Gradle dependencies from: {}", buildFile);

            String buildContent = Files.readString(buildFile);

            // Extract dependencies using regex patterns
            Matcher matcher = GRADLE_DEP_PATTERN.matcher(buildContent);

            while (matcher.find()) {
                String configuration = matcher.group(1);
                String dependency = matcher.group(2);

                // Skip test configurations
                if (configuration.toLowerCase().contains("test")) {
                    continue;
                }

                // Parse group:artifact:version format
                Matcher gavMatcher = GRADLE_GROUP_ARTIFACT_VERSION.matcher(dependency);
                if (gavMatcher.matches()) {
                    String groupId = gavMatcher.group(1);
                    String artifactId = gavMatcher.group(2);
                    String version = gavMatcher.group(3);

                    String jarPath = downloadMavenArtifact(groupId, artifactId, version);
                    if (jarPath != null) {
                        jarPaths.add(jarPath);
                    }
                }
            }

            log.info("Resolved {} Gradle dependencies", jarPaths.size());

        } catch (Exception e) {
            log.error("Error resolving Gradle dependencies: {}", e.getMessage(), e);
        }

        return jarPaths;
    }

    /**
     * Downloads a Maven artifact from repositories
     */
    private String downloadMavenArtifact(String groupId, String artifactId, String version) {
        try {
            // Check if already exists in local Maven repository
            String userHome = System.getProperty("user.home");
            Path localRepo = Paths.get(userHome, ".m2", "repository");
            Path artifactPath = localRepo.resolve(groupId.replace('.', '/'))
                    .resolve(artifactId)
                    .resolve(version)
                    .resolve(artifactId + "-" + version + ".jar");

            if (Files.exists(artifactPath)) {
                log.debug("Found local artifact: {}", artifactPath);
                return artifactPath.toString();
            }

            // Try to download from Maven Central
            String[] repositories = {
                    "https://repo.maven.apache.org/maven2",
                    "https://repository.apache.org/content/repositories/releases",
                    "https://repo.spring.io/release"
            };

            for (String repoUrl : repositories) {
                String downloadUrl = constructMavenUrl(repoUrl, groupId, artifactId, version);

                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(downloadUrl))
                            .timeout(Duration.ofSeconds(30))
                            .GET()
                            .build();

                    HttpResponse<byte[]> response = httpClient.send(request,
                            HttpResponse.BodyHandlers.ofByteArray());

                    if (response.statusCode() == 200) {
                        // Create directories
                        Files.createDirectories(artifactPath.getParent());

                        // Write the JAR file
                        Files.write(artifactPath, response.body());

                        log.info("Downloaded artifact: {}:{} from {}", groupId, artifactId, repoUrl);
                        return artifactPath.toString();
                    }
                } catch (Exception e) {
                    log.debug("Failed to download from {}: {}", repoUrl, e.getMessage());
                }
            }

            log.warn("Could not download artifact: {}:{}:{}", groupId, artifactId, version);

        } catch (Exception e) {
            log.error("Error downloading artifact {}:{}:{}: {}",
                    groupId, artifactId, version, e.getMessage());
        }

        return null;
    }

    /**
     * Constructs Maven repository URL for artifact
     */
    private String constructMavenUrl(String baseUrl, String groupId, String artifactId, String version) {
        String groupPath = groupId.replace('.', '/');
        return String.format("%s/%s/%s/%s/%s-%s.jar",
                baseUrl, groupPath, artifactId, version, artifactId, version);
    }

    /**
     * Extracts Maven properties from pom.xml
     */
    private Map<String, String> extractMavenProperties(Document doc) {
        Map<String, String> properties = new HashMap<>();

        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            Element propertiesElement = (Element) propertiesNodes.item(0);
            NodeList propertyNodes = propertiesElement.getChildNodes();

            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Node propertyNode = propertyNodes.item(i);
                if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element property = (Element) propertyNode;
                    String name = property.getTagName();
                    String value = property.getTextContent();
                    properties.put(name, value);
                }
            }
        }

        // Add some common Spring Boot properties
        if (!properties.containsKey("spring-boot.version")) {
            properties.put("spring-boot.version", "3.3.5");
        }
        if (!properties.containsKey("spring.version")) {
            properties.put("spring.version", "6.1.13");
        }

        return properties;
    }

    /**
     * Gets element text content safely
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    /**
     * Finds the project root directory by looking for build files
     */
    private Path findProjectRoot(Path filePath) {
        Path current = filePath.getParent();

        while (current != null) {
            if (Files.exists(current.resolve("pom.xml")) ||
                    Files.exists(current.resolve("build.gradle")) ||
                    Files.exists(current.resolve("build.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }

        return null;
    }

    /**
     * Finds all Java files in the project
     */
    private List<Path> findAllJavaFiles(Path projectRoot) throws IOException {
        try (Stream<Path> walk = Files.walk(projectRoot)) {
            return walk.filter(path -> path.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Finds source directories in the project
     */
    private List<Path> findSourceDirectories(Path projectRoot) {
        List<Path> sourceDirs = new ArrayList<>();

        // Standard Maven/Gradle source directories
        Path srcMainJava = projectRoot.resolve("src/main/java");
        Path srcTestJava = projectRoot.resolve("src/test/java");

        if (Files.isDirectory(srcMainJava)) {
            sourceDirs.add(srcMainJava);
        }
        if (Files.isDirectory(srcTestJava)) {
            sourceDirs.add(srcTestJava);
        }

        // Try to find other source directories
        try (Stream<Path> walk = Files.walk(projectRoot, 3)) {
            walk.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals("java"))
                    .filter(path -> !sourceDirs.contains(path))
                    .forEach(sourceDirs::add);
        } catch (IOException e) {
            log.warn("Error finding source directories: {}", e.getMessage());
        }

        return sourceDirs;
    }

    /**
     * Removes duplicate dependencies and merges types within same package
     */
    private List<Dependency> deduplicateDependencies(List<Dependency> dependencies) {
        Map<String, Dependency> uniqueDeps = new LinkedHashMap<>();
        for (Dependency dep : dependencies) {
            String packageName = dep.getPackageName();
            Dependency existing = uniqueDeps.get(packageName);
            if (existing == null) {
                uniqueDeps.put(packageName, dep);
            } else {
                // Merge types from duplicate packages
                for (tcc.com.viewer.domains.dependency.Type type : dep.getTypes()) {
                    boolean typeExists = existing.getTypes().stream()
                            .anyMatch(t -> t.getFullyQualifiedName().equals(type.getFullyQualifiedName()));
                    if (!typeExists) {
                        existing.addType(type);
                    }
                }
            }
        }
        return new ArrayList<>(uniqueDeps.values());
    }

    /**
     * Custom visitor for extracting dependencies using JavaParser
     */
    private class JavaParserDependencyVisitor extends VoidVisitorAdapter<Void> {
        private final List<Dependency> dependencies = new ArrayList<>();
        private final Map<String, Dependency> packageToDependencyMap = new HashMap<>();
        private final Map<String, String> importMap = new HashMap<>();
        private final Set<String> onDemandImports = new HashSet<>();
        private final CombinedTypeSolver typeSolver;
        private String currentPackage = "";
        private String currentClassName = "";

        public JavaParserDependencyVisitor(CombinedTypeSolver typeSolver) {
            this.typeSolver = typeSolver;
        }

        public List<Dependency> getDependencies() {
            return dependencies;
        }

        @Override
        public void visit(PackageDeclaration n, Void arg) {
            currentPackage = n.getNameAsString();
            super.visit(n, arg);
        }

        @Override
        public void visit(ImportDeclaration n, Void arg) {
            String importName = n.getNameAsString();

            if (n.isAsterisk()) {
                // Wildcard import (e.g., jakarta.persistence.*)
                onDemandImports.add(importName);
                log.debug("Added wildcard import: {}", importName);
            } else {
                // Explicit import
                String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
                importMap.put(simpleName, importName);

                // Add as dependency
                addDependencyIfNotExists(importName);
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            // Track current class name to avoid self-references
            currentClassName = n.getNameAsString();

            // Process class annotations - this was missing and causing test failures!
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            // Handle extended types (superclass)
            for (ClassOrInterfaceType extendedType : n.getExtendedTypes()) {
                processType(extendedType);
            }

            // Handle implemented types (interfaces)
            for (ClassOrInterfaceType implementedType : n.getImplementedTypes()) {
                processType(implementedType);
            }

            // Handle type parameters (generics)
            for (TypeParameter typeParam : n.getTypeParameters()) {
                for (ClassOrInterfaceType bound : typeParam.getTypeBound()) {
                    processType(bound);
                }
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(EnumDeclaration n, Void arg) {
            currentClassName = n.getNameAsString();

            // Process enum annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            // Handle implemented interfaces
            for (ClassOrInterfaceType implementedType : n.getImplementedTypes()) {
                processType(implementedType);
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(RecordDeclaration n, Void arg) {
            currentClassName = n.getNameAsString();

            // Process record annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            // Handle implemented interfaces
            for (ClassOrInterfaceType implementedType : n.getImplementedTypes()) {
                processType(implementedType);
            }

            // Handle record parameters
            for (Parameter param : n.getParameters()) {
                processType(param.getType());

                // Process parameter annotations
                for (AnnotationExpr annotation : param.getAnnotations()) {
                    processAnnotation(annotation);
                }
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(AnnotationDeclaration n, Void arg) {
            currentClassName = n.getNameAsString();

            // Process annotation declaration annotations (meta-annotations)
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(FieldDeclaration n, Void arg) {
            // Process field type
            processType(n.getCommonType());

            // Process field annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            // Process return type
            processType(n.getType());

            // Process parameter types
            for (Parameter param : n.getParameters()) {
                processType(param.getType());

                // Process parameter annotations
                for (AnnotationExpr annotation : param.getAnnotations()) {
                    processAnnotation(annotation);
                }
            }

            // Process thrown exceptions
            for (ReferenceType thrownException : n.getThrownExceptions()) {
                processType(thrownException);
            }

            // Process method annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            // Process type parameters (generic methods)
            for (TypeParameter typeParam : n.getTypeParameters()) {
                for (ClassOrInterfaceType bound : typeParam.getTypeBound()) {
                    processType(bound);
                }
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            // Process parameter types
            for (Parameter param : n.getParameters()) {
                processType(param.getType());

                // Process parameter annotations
                for (AnnotationExpr annotation : param.getAnnotations()) {
                    processAnnotation(annotation);
                }
            }

            // Process thrown exceptions
            for (ReferenceType thrownException : n.getThrownExceptions()) {
                processType(thrownException);
            }

            // Process constructor annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(VariableDeclarationExpr n, Void arg) {
            // Process variable type
            processType(n.getCommonType());

            // Process annotations
            for (AnnotationExpr annotation : n.getAnnotations()) {
                processAnnotation(annotation);
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(ObjectCreationExpr n, Void arg) {
            // Process the type being instantiated
            processType(n.getType());

            super.visit(n, arg);
        }

        @Override
        public void visit(CastExpr n, Void arg) {
            // Process the cast target type
            processType(n.getType());

            super.visit(n, arg);
        }

        @Override
        public void visit(InstanceOfExpr n, Void arg) {
            // Process the type being checked
            processType(n.getType());

            super.visit(n, arg);
        }

        @Override
        public void visit(CatchClause n, Void arg) {
            // Process exception parameter type
            processType(n.getParameter().getType());

            super.visit(n, arg);
        }

        @Override
        public void visit(ForEachStmt n, Void arg) {
            // Process variable type in for-each loop
            processType(n.getVariable().getCommonType());

            super.visit(n, arg);
        }

        @Override
        public void visit(LambdaExpr n, Void arg) {
            // Process lambda parameter types
            for (Parameter param : n.getParameters()) {
                processType(param.getType());
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(MethodReferenceExpr n, Void arg) {
            // Method references can contain type information
            try {
                ResolvedType resolvedType = n.calculateResolvedType();
                processResolvedType(resolvedType);
            } catch (Exception e) {
                log.debug("Could not resolve method reference type: {}", e.getMessage());
            }

            super.visit(n, arg);
        }

        /**
         * Processes any Type node to extract dependencies
         * Fixed to handle wildcard import failures with direct TypeSolver resolution
         */
        private void processType(Type type) {
            if (type == null) return;

            try {
                // Try JavaParser resolution first (works for explicit imports)
                ResolvedType resolvedType = type.resolve();
                processResolvedType(resolvedType);
            } catch (Exception e) {
                log.debug("JavaParser resolution failed for type, trying direct TypeSolver: {}", e.getMessage());

                // JavaParser resolution failed - use direct TypeSolver lookup for ClassOrInterfaceType
                if (type instanceof ClassOrInterfaceType) {
                    String typeName = ((ClassOrInterfaceType) type).getNameAsString();
                    String fullyQualifiedName = resolveTypeWithTypeSolver(typeName);
                    if (fullyQualifiedName != null) {
                        log.debug("Resolved type {} to {} via direct TypeSolver", typeName, fullyQualifiedName);
                        addDependencyIfNotExists(fullyQualifiedName);

                        // Process type arguments even when main type resolution succeeded via TypeSolver
                        ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
                        if (classType.getTypeArguments().isPresent()) {
                            for (Type typeArg : classType.getTypeArguments().get()) {
                                processType(typeArg);
                            }
                        }
                        return;
                    }
                }

                // Continue with existing AST fallback for other cases
                processTypeFromAST(type);
            }
        }

        /**
         * Processes a resolved type to extract fully qualified names
         */
        private void processResolvedType(ResolvedType resolvedType) {
            if (resolvedType == null) return;

            try {
                if (resolvedType.isReferenceType()) {
                    String qualifiedName = resolvedType.asReferenceType().getQualifiedName();

                    // Check if this is a generic container that should be skipped
                    boolean isGenericContainer = isGenericContainer(qualifiedName);
                    boolean hasTypeParameters = false;

                    // Check for type parameters
                    try {
                        for (ResolvedType typeParam : resolvedType.asReferenceType().typeParametersValues()) {
                            hasTypeParameters = true;
                            processResolvedType(typeParam);
                        }
                    } catch (Exception e) {
                        // Type might not have type parameters - ignore
                        log.debug("No type parameters for {}: {}", qualifiedName, e.getMessage());
                    }

                    // Only add the container type if it's not a generic container OR has no type parameters
                    if (!isGenericContainer || !hasTypeParameters) {
                        addDependencyIfNotExists(qualifiedName);
                    } else {
                        log.debug("Skipping generic container (resolved): {}", qualifiedName);
                    }

                } else if (resolvedType.isArray()) {
                    // For arrays, extract the component type
                    processResolvedType(resolvedType.asArrayType().getComponentType());
                }
            } catch (Exception e) {
                log.debug("Error processing resolved type: {}", e.getMessage());
            }
        }

        /**
         * Fallback method to process types from AST when resolution fails
         */
        private void processTypeFromAST(Type type) {
            if (type instanceof ClassOrInterfaceType) {
                ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
                String typeName = classType.getNameAsString();

                // Try to resolve using direct TypeSolver
                String fullyQualifiedName = resolveTypeWithTypeSolver(typeName);

                // Check if this is a generic container that should be skipped
                if (fullyQualifiedName != null) {
                    boolean isGenericContainer = isGenericContainer(fullyQualifiedName);

                    // Only add the container type if it's not a generic container OR has no type arguments
                    if (!isGenericContainer || !classType.getTypeArguments().isPresent()) {
                        addDependencyIfNotExists(fullyQualifiedName);
                    } else {
                        log.debug("Skipping generic container: {}", fullyQualifiedName);
                    }
                }

                // Process type arguments (generics) - this is where we extract the actual dependencies
                if (classType.getTypeArguments().isPresent()) {
                    log.debug("Processing type arguments for: {}", typeName);
                    for (Type typeArg : classType.getTypeArguments().get()) {
                        processType(typeArg);
                    }
                }

                // Process scope (for nested classes like OuterClass.InnerClass)
                if (classType.getScope().isPresent()) {
                    processType(classType.getScope().get());
                }

            } else if (type instanceof ArrayType) {
                // For arrays, extract the component type
                ArrayType arrayType = (ArrayType) type;
                processType(arrayType.getComponentType());
            } else if (type instanceof WildcardType) {
                // Process wildcard bounds (? extends SomeType, ? super SomeType)
                WildcardType wildcardType = (WildcardType) type;
                if (wildcardType.getExtendedType().isPresent()) {
                    processType(wildcardType.getExtendedType().get());
                }
                if (wildcardType.getSuperType().isPresent()) {
                    processType(wildcardType.getSuperType().get());
                }
            }
        }

        /**
         * Checks if a type is a generic container that should be skipped in favor of its type parameters
         */
        private boolean isGenericContainer(String fullyQualifiedName) {
            Set<String> genericContainers = Set.of(
                    "java.util.List", "java.util.Map", "java.util.Set",
                    "java.util.Collection", "java.util.Optional", "java.util.ArrayList",
                    "java.util.HashMap", "java.util.HashSet", "java.util.LinkedList",
                    "java.util.TreeSet", "java.util.TreeMap"
            );

            return genericContainers.contains(fullyQualifiedName) ||
                    (fullyQualifiedName != null && (
                            fullyQualifiedName.equals("List") || fullyQualifiedName.equals("Map") ||
                                    fullyQualifiedName.equals("Set") || fullyQualifiedName.equals("Collection") ||
                                    fullyQualifiedName.equals("Optional")
                    ));
        }

        /**
         * Processes annotations to extract their types
         * Fixed to use direct TypeSolver resolution instead of broken JavaParser resolution
         */
        private void processAnnotation(AnnotationExpr annotation) {
            if (annotation == null) return;

            String annotationName = annotation.getNameAsString();

            // Don't try JavaParser resolution - it's broken for marker annotations
            // Go directly to our resolution logic
            String fullyQualifiedName = resolveTypeWithTypeSolver(annotationName);
            if (fullyQualifiedName != null) {
                log.debug("Resolved annotation {} to {}", annotationName, fullyQualifiedName);
                addDependencyIfNotExists(fullyQualifiedName);
            } else {
                log.debug("Could not resolve annotation: {}", annotationName);
            }

            // Process annotation member values
            if (annotation instanceof NormalAnnotationExpr) {
                NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
                for (MemberValuePair pair : normalAnnotation.getPairs()) {
                    processAnnotationValue(pair.getValue());
                }
            } else if (annotation instanceof SingleMemberAnnotationExpr) {
                SingleMemberAnnotationExpr singleAnnotation = (SingleMemberAnnotationExpr) annotation;
                processAnnotationValue(singleAnnotation.getMemberValue());
            }
        }

        /**
         * Processes annotation values that might contain type references
         */
        private void processAnnotationValue(Expression value) {
            if (value instanceof ClassExpr) {
                ClassExpr classExpr = (ClassExpr) value;
                processType(classExpr.getType());
            } else if (value instanceof ArrayInitializerExpr) {
                ArrayInitializerExpr arrayInit = (ArrayInitializerExpr) value;
                for (Expression element : arrayInit.getValues()) {
                    processAnnotationValue(element);
                }
            } else if (value instanceof FieldAccessExpr) {
                // Handle static enum references like GenerationType.UUID
                FieldAccessExpr fieldAccess = (FieldAccessExpr) value;
                try {
                    // Try to resolve the scope (the enum class)
                    ResolvedType resolvedType = fieldAccess.getScope().calculateResolvedType();
                    processResolvedType(resolvedType);
                } catch (Exception e) {
                    // Fallback: extract the type name from the scope
                    if (fieldAccess.getScope() instanceof NameExpr) {
                        String scopeName = ((NameExpr) fieldAccess.getScope()).getNameAsString();
                        String fullyQualifiedName = resolveTypeWithTypeSolver(scopeName);
                        if (fullyQualifiedName != null) {
                            log.debug("Resolved enum type {} to {} via field access", scopeName, fullyQualifiedName);
                            addDependencyIfNotExists(fullyQualifiedName);
                        }
                    }
                }
            } else if (value instanceof NameExpr) {
                // Handle simple enum references
                NameExpr nameExpr = (NameExpr) value;
                String name = nameExpr.getNameAsString();
                String fullyQualifiedName = resolveTypeWithTypeSolver(name);
                if (fullyQualifiedName != null) {
                    addDependencyIfNotExists(fullyQualifiedName);
                }
            }
        }

        /**
         * Resolves a simple type name to its fully qualified name using direct TypeSolver queries
         * This bypasses JavaParser's broken resolution for wildcard imports
         */
        private String resolveTypeWithTypeSolver(String simpleName) {
            if (simpleName == null || simpleName.isEmpty()) {
                return null;
            }

            // 1. Check explicit imports first
            if (importMap.containsKey(simpleName)) {
                return importMap.get(simpleName);
            }

            // 2. Check wildcard imports using direct TypeSolver queries
            for (String wildcardImport : onDemandImports) {
                String candidateFQN = wildcardImport + "." + simpleName;
                try {
                    var resolved = typeSolver.tryToSolveType(candidateFQN);
                    if (resolved.isSolved()) {
                        log.debug("Resolved {} to {} via wildcard import {}", simpleName, candidateFQN, wildcardImport);
                        return candidateFQN;
                    }
                } catch (Exception e) {
                    // Continue to next wildcard import
                }
            }

            // 3. Check same package
            if (!currentPackage.isEmpty()) {
                String candidateFQN = currentPackage + "." + simpleName;
                try {
                    var resolved = typeSolver.tryToSolveType(candidateFQN);
                    if (resolved.isSolved()) {
                        log.debug("Resolved {} to {} via same package", simpleName, candidateFQN);
                        return candidateFQN;
                    }
                } catch (Exception e) {
                    // Continue
                }
            }

            // 4. Check java.lang
            String javaLangCandidate = "java.lang." + simpleName;
            try {
                var resolved = typeSolver.tryToSolveType(javaLangCandidate);
                if (resolved.isSolved()) {
                    log.debug("Resolved {} to {} via java.lang", simpleName, javaLangCandidate);
                    return javaLangCandidate;
                }
            } catch (Exception e) {
                // Continue
            }

            log.debug("Could not resolve type: {}", simpleName);
            return null; // Could not resolve
        }

        /**
         * Legacy method - now delegates to resolveTypeWithTypeSolver
         */
        private String resolveTypeFromImports(String simpleName) {
            String resolved = resolveTypeWithTypeSolver(simpleName);
            return resolved != null ? resolved : simpleName;
        }

        private boolean isJavaLangClass(String className) {
            Set<String> javaLangClasses = Set.of(
                    "Object", "String", "Integer", "Long", "Double", "Float",
                    "Boolean", "Byte", "Short", "Character", "Number", "Class",
                    "Exception", "RuntimeException", "Error", "Throwable",
                    "Thread", "Runnable", "System", "Math"
            );
            return javaLangClasses.contains(className);
        }

        private void addDependencyIfNotExists(String fullyQualifiedName) {
            if (isSignificantDependency(fullyQualifiedName)) {
                String packageName = packageNameExtractor.extractPackageName(fullyQualifiedName);
                tcc.com.viewer.domains.dependency.Type type = new tcc.com.viewer.domains.dependency.Type(fullyQualifiedName);

                // Get or create dependency for this package
                Dependency dependency = packageToDependencyMap.get(packageName);
                if (dependency == null) {
                    dependency = new Dependency(packageName);
                    packageToDependencyMap.put(packageName, dependency);
                    dependencies.add(dependency);
                    log.debug("Added package dependency: {}", packageName);
                }

                // Add type to the dependency if not already present
                boolean typeExists = dependency.getTypes().stream()
                        .anyMatch(t -> t.getFullyQualifiedName().equals(fullyQualifiedName));

                if (!typeExists) {
                    dependency.addType(type);
                    log.debug("Added type {} to package {}", fullyQualifiedName, packageName);
                }
            }
        }

        private boolean isSignificantDependency(String fullyQualifiedName) {
            if (fullyQualifiedName == null || fullyQualifiedName.isEmpty()) {
                return false;
            }

            // Skip primitives and wrappers
            Set<String> primitiveTypes = Set.of("int", "long", "double", "float", "boolean",
                    "byte", "short", "char", "void",
                    "String", "Integer", "Long", "Double", "Float",
                    "Boolean", "Byte", "Short", "Character");

            String simpleName = fullyQualifiedName.contains(".") ?
                    fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1) :
                    fullyQualifiedName;

            if (primitiveTypes.contains(simpleName)) {
                return false;
            }

            // Skip generic containers - we extract their type parameters instead
            Set<String> genericContainers = Set.of(
                    "java.util.List", "java.util.Map", "java.util.Set",
                    "java.util.Collection", "java.util.Optional",
                    "List", "Map", "Set", "Collection", "Optional"
            );

            if (genericContainers.contains(fullyQualifiedName)) {
                return false;
            }

            // Skip self-references
            if (fullyQualifiedName.equals(currentClassName) ||
                    fullyQualifiedName.endsWith("." + currentClassName)) {
                return false;
            }

            return true;
        }
    }
}