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

    /**
     * Parses a Java file and returns its AST
     */
    public CompilationUnit parseClass(Path classPath) throws IOException {
        String source = Files.readString(classPath);
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // Use the latest supported JLS level

        // Set parser options
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        // Set up compiler options
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_19, options); // Adjust version as needed
        parser.setCompilerOptions(options);

        // Determine the project directory being analyzed
        Path projectDir = findProjectRoot(classPath);

        // Set up the environment with improved classpath and sourcepath
        String[] classpath = getComprehensiveClassPath(projectDir);
        String[] sourcepath = getComprehensiveSourcePath(projectDir);

        System.out.println("Using classpath with " + classpath.length + " entries");
        System.out.println("Using sourcepath with " + sourcepath.length + " entries");

        parser.setEnvironment(classpath, sourcepath, null, true);
        parser.setUnitName(classPath.getFileName().toString());

        return (CompilationUnit) parser.createAST(null);
    }

    /**
     * Gets the classpath from the project's Maven dependencies
     */
//    private String[] getClassPath(String projectPath) {
//        // For a real implementation, this would dynamically find all JARs in the Maven repo
//        // For simplicity, we're just using a placeholder
//
//        // Look for .m2 repository or lib directories
//        List<String> classpath = new ArrayList<>();
//
//        // Add the maven repository classpath entries
//        String userHome = System.getProperty("user.home");
//        Path m2Path = Path.of(userHome, ".m2", "repository");
//        classpath.add(m2Path.toString());
//
//        // Add the standard JRE libraries
//        String javaHome = System.getProperty("java.home");
//        classpath.add(javaHome + "/jmods");
//
//        // Add the project's target/classes directory
//        classpath.add(projectPath + "/target/classes");
//
//        // This is a simplification - in practice you'd scan for all relevant JARs
//        return classpath.toArray(new String[0]);
//    }

    /**
     * Gets the source paths for the project
     */
    private String[] getSourcePath(String projectPath) {
        return new String[]{
                projectPath + "/src/main/java",
                projectPath + "/src/test/java"
        };
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

        } catch (Exception e) {
            System.err.println("Error building classpath: " + e.getMessage());
            e.printStackTrace();
        }

        return classpath.toArray(new String[0]);
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

            // Get all dependencies
            NodeList dependencyNodes = doc.getElementsByTagName("dependency");

            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node node = dependencyNodes.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String groupId = getElementTextContent(element, "groupId");
                    String artifactId = getElementTextContent(element, "artifactId");
                    String version = getElementTextContent(element, "version");

                    // Skip dependencies with placeholders or properties
                    if (groupId.contains("${") || artifactId.contains("${") || version == null || version.contains("${")) {
                        System.out.println("Skipping dependency with placeholder: " + groupId + ":" + artifactId + ":" + version);
                        continue;
                    }

                    // Ignore test scope dependencies
                    String scope = getElementTextContent(element, "scope");
                    if ("test".equals(scope)) {
                        continue;
                    }

                    addMavenDependencyToClasspath(groupId, artifactId, version, classpath);
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

                    if (groupId != null && artifactId != null && version != null) {
                        addMavenDependencyToClasspath(groupId, artifactId, version, classpath);
                    }
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("Error parsing pom.xml: " + e.getMessage());
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

        String userHome = System.getProperty("user.home");
        Path m2Path = Paths.get(userHome, ".m2", "repository");

        // Convert group ID to path
        String groupPath = groupId.replace('.', '/');

        // Construct the path to the JAR file
        Path jarPath = m2Path.resolve(Paths.get(groupPath, artifactId, version,
                artifactId + "-" + version + ".jar"));

        if (Files.exists(jarPath)) {
            classpath.add(jarPath.toString());
            System.out.println("Added Maven dependency: " + groupId + ":" + artifactId + ":" + version);
        } else {
            System.out.println("Maven dependency not found: " + jarPath);
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
     * Attempts to determine the project root path based on the class file path
     */
//    private String getProjectRootPath(Path classPath) {
//        // Navigate up the directory hierarchy until we find pom.xml or build.gradle
//        Path current = classPath.getParent();
//        while (current != null) {
//            if (Files.exists(current.resolve("pom.xml")) ||
//                    Files.exists(current.resolve("build.gradle"))) {
//                return current.toString();
//            }
//            current = current.getParent();
//        }
//
//        // Fallback: assume the parent of src is the project root
//        current = classPath.getParent();
//        while (current != null) {
//            if (current.getFileName().toString().equals("src")) {
//                return current.getParent().toString();
//            }
//            current = current.getParent();
//        }
//
//        // Last resort: use the parent directory of the class file
//        assert classPath.getParent() != null;
//        return classPath.getParent().toString();
//    }

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
        // If no build file found, return the /uploads directory or a reasonable default
        return Paths.get("/uploads");
    }

    /**
     * Extracts all dependencies from a Java class file
     */
    public List<Dependency> getDependencies(Path classPath) {
        TypeDependencyVisitor visitor = new TypeDependencyVisitor();

        try {
            CompilationUnit cu = parseClass(classPath);
            // Create a visitor to collect type references
            cu.accept(visitor);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return visitor.getDependencies();
    }

    /**
     * ASTVisitor implementation to collect type references
     */
    @Getter
    private static class TypeDependencyVisitor extends ASTVisitor {
        private final List<Dependency> dependencies = new ArrayList<>();

//        decide to either use this or not. the problem here is that the `OnDemand`
//        import can resolve to either a package or a type, and we don't want a package.
//        @Override
//        public boolean visit(ImportDeclaration node) {
//            node.
//            if (node.isOnDemand()) {
//                // Wildcard import (e.g., java.util.*)
//                node.resolveBinding().
//                wildcardImports.add(node.getName().getFullyQualifiedName());
//            } else {
//                // Explicit import
//                explicitImports.add(node.getName().getFullyQualifiedName());
//            }
//            return super.visit(node);
//        }

        @Override
        public boolean visit(TypeDeclaration node) {
            // Handle superclass
            if (node.getSuperclassType() != null) {
                ITypeBinding binding = node.getSuperclassType().resolveBinding();
                if (binding != null) {
                    addDependencyIfNotExists(new Dependency(binding.getQualifiedName()));
                }
            }

            // Handle implemented interfaces
            for (Object o : node.superInterfaceTypes()) {
                Type interfaceType = (Type) o;
                ITypeBinding binding = interfaceType.resolveBinding();
                if (binding != null) {
                    addDependencyIfNotExists(new Dependency(binding.getQualifiedName()));
                }
            }

            return super.visit(node);
        }

        /**
         * Visit a Record declaration (Java 16+)
         */
        @Override
        public boolean visit(RecordDeclaration node) {
            // Handle implemented interfaces
            for (Object o : node.superInterfaceTypes()) {
                Type interfaceType = (Type) o;
                ITypeBinding binding = interfaceType.resolveBinding();
                if (binding != null) {
                    addDependencyIfNotExists(new Dependency(binding.getQualifiedName()));
                }
            }

            // Record parameter types
            for (Object o : node.typeParameters()) {
                SingleVariableDeclaration param = (SingleVariableDeclaration) o;
                ITypeBinding paramType = param.getType().resolveBinding();
                if (paramType != null) {
                    addTypeAndGenerics(paramType);
                }
            }

            // Handle record components
            for (Object o : node.recordComponents()) {
                SingleVariableDeclaration component = (SingleVariableDeclaration) o;
                ITypeBinding binding = component.getType().resolveBinding();
                if (binding != null) {
                    addTypeAndGenerics(binding);
                }
            }

            return super.visit(node);
        }

        /**
         * Visit an Annotation declaration
         */
        @Override
        public boolean visit(AnnotationTypeDeclaration node) {
            // Process annotation type members
            for (Object o : node.bodyDeclarations()) {
                if (o instanceof AnnotationTypeMemberDeclaration member) {
                    ITypeBinding binding = member.getType().resolveBinding();
                    if (binding != null) {
                        addTypeAndGenerics(binding);
                    }
                }
            }

            return super.visit(node);
        }

        /**
         * Visit a SingleMemberAnnotation declaration
         */
        @Override
        public boolean visit(SingleMemberAnnotation node) {
            // Process annotation type members
            ITypeBinding binding = node.resolveTypeBinding();
            if (binding != null) {
                addTypeAndGenerics(binding);
            }

            return super.visit(node);
        }

        /**
         * Visit a MarkerAnnotation declaration
         */
        @Override
        public boolean visit(MarkerAnnotation node) {
            // Process annotation type members
            ITypeBinding binding = node.resolveTypeBinding();
            if (binding != null) {
                addTypeAndGenerics(binding);
            }

            return super.visit(node);
        }

        /**
         * Visit a NormalAnnotation declaration
         */
        @Override
        public boolean visit(NormalAnnotation node) {
            // Process annotation type members
            ITypeBinding binding = node.resolveTypeBinding();
            if (binding != null) {
                addTypeAndGenerics(binding);
            }

            return super.visit(node);
        }

        @Override
        public boolean visit(FieldDeclaration node) {
            ITypeBinding binding = node.getType().resolveBinding();
            if (binding != null) {
                addTypeAndGenerics(binding);
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            // Return type
            if (node.getReturnType2() != null) {
                ITypeBinding returnType = node.getReturnType2().resolveBinding();
                if (returnType != null && !returnType.isPrimitive()) {
                    addTypeAndGenerics(returnType);
                }
            }

            // Parameter types
            for (Object o : node.parameters()) {
                SingleVariableDeclaration param = (SingleVariableDeclaration) o;
                ITypeBinding paramType = param.getType().resolveBinding();
                if (paramType != null) {
                    addTypeAndGenerics(paramType);
                }
            }

            // Thrown exceptions
            for (Object o : node.thrownExceptionTypes()) {
                Type exceptionType = (Type) o;
                ITypeBinding binding = exceptionType.resolveBinding();
                if (binding != null) {
                    addTypeAndGenerics(binding);
                }
            }

            return super.visit(node);
        }

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            ITypeBinding binding = node.getType().resolveBinding();
            if (binding != null) {
                addTypeAndGenerics(binding);
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(ClassInstanceCreation node) {
            ITypeBinding binding = node.getType().resolveBinding();
            if (binding != null) {
                addTypeAndGenerics(binding);
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodInvocation node) {
            // If there's a target expression, get its type as a dependency
            if (node.getExpression() != null) {
                ITypeBinding binding = node.getExpression().resolveTypeBinding();
                if (binding != null) {
                    addTypeAndGenerics(binding);
                }
            }
            return super.visit(node);
        }

        /**
         * Helper method to check if a dependency already exists and add it if not
         */
        private void addDependencyIfNotExists(Dependency dependency) {
            if (dependency.dependencyDoesNotExist(dependencies)) {
                dependencies.add(dependency);
            }
        }

        /**
         * Helper method to add a type and its generic type arguments
         */
        private void addTypeAndGenerics(ITypeBinding binding) {
            // Add the base type
            addDependencyIfNotExists(new Dependency(binding.getQualifiedName()));

            // Add generic type arguments if any
            if (binding.isParameterizedType()) {
                for (ITypeBinding typeArg : binding.getTypeArguments()) {
                    if (!typeArg.isPrimitive()) {
                        addDependencyIfNotExists(new Dependency(typeArg.getQualifiedName()));
                    }
                }
            }
        }
    }
}