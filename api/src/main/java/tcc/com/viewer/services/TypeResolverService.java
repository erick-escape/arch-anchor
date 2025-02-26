package tcc.com.viewer.services;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import tcc.com.viewer.antlr4.JavaLexer;
import tcc.com.viewer.antlr4.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TypeResolverService {
    private final Map<String, String> typeResolutions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> packageToTypes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> fileImports = new ConcurrentHashMap<>();
    private final Map<String, String> filePackages = new ConcurrentHashMap<>();

    // Cache for nested types (format: "OuterClass$InnerClass" -> "fully.qualified.OuterClass$InnerClass")
    private final Map<String, String> nestedTypeCache = new ConcurrentHashMap<>();

    // Cache for parameterized types (format: "List<String>" -> "java.util.List<java.lang.String>")
    private final Map<String, String> parameterizedTypeCache = new ConcurrentHashMap<>();

    /**
     * Scans the entire project directory to build the type resolution cache
     */
    public void scanProject(Path projectRoot) throws IOException {
        // Walk through all Java files in the project
        Files.walk(projectRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(this::processJavaFile);

        // Second pass to resolve nested types and type parameters
        resolveNestedAndParameterizedTypes();
    }

    private void processJavaFile(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            JavaLexer lexer = new JavaLexer(CharStreams.fromString(content));
            JavaParser parser = new JavaParser(new CommonTokenStream(lexer));
            ParseTree tree = parser.compilationUnit();

            // Create a visitor to extract type information
            TypeExtractorVisitorService visitor = new TypeExtractorVisitorService(javaFile);
            visitor.visit(tree);

            // Store the extracted information
            String packageName = visitor.getPackageName();
            filePackages.put(javaFile.toString(), packageName);
            fileImports.put(javaFile.toString(), visitor.getImports());

            // Add types to package mapping
            Set<String> typesInPackage = packageToTypes.computeIfAbsent(
                    packageName, k -> new HashSet<>());
            typesInPackage.addAll(visitor.getDeclaredTypes());

            // Pre-populate resolution cache for declared types
            for (String type : visitor.getDeclaredTypes()) {
                typeResolutions.put(type, packageName + "." + type);
            }
        } catch (IOException e) {
            System.out.println("processJavaFile method: " + e.getMessage());
        }
    }

    private void resolveNestedAndParameterizedTypes() {
        // Process nested types
        for (Map.Entry<String, Set<String>> entry : packageToTypes.entrySet()) {
            String packageName = entry.getKey();
            for (String type : entry.getValue()) {
                if (type.contains("$")) {
                    String[] parts = type.split("\\$");
                    String outerType = parts[0];
                    String nestedType = parts[1];
                    nestedTypeCache.put(
                            nestedType,
                            packageName + "." + outerType + "$" + nestedType
                    );
                }
            }
        }
    }

    /**
     * Resolves a type, handling complex cases like nested types and generics
     */
    public String resolveType(String type, String currentFile) {
        // Check if it's a parameterized type
        if (type.contains("<")) {
            return resolveParameterizedType(type, currentFile);
        }

        // Check resolution cache first
        String resolved = typeResolutions.get(type);
        if (resolved != null) {
            return resolved;
        }

        // Check nested type cache
        resolved = nestedTypeCache.get(type);
        if (resolved != null) {
            return resolved;
        }

        // Get current file's package and imports
        String currentPackage = filePackages.get(currentFile);
        List<String> imports = fileImports.get(currentFile);

        // Resolution logic following Java's type resolution rules
        resolved = resolveTypeFollowingJavaRules(type, currentPackage, imports);
        if (resolved != null) {
            typeResolutions.put(type, resolved);
        }

        return resolved;
    }

    private String resolveParameterizedType(String type, String currentFile) {
        String cached = parameterizedTypeCache.get(type);
        if (cached != null) {
            return cached;
        }

        // Extract the raw type and type parameters
        int genericStart = type.indexOf('<');
        String rawType = type.substring(0, genericStart);
        String typeParams = type.substring(genericStart + 1, type.length() - 1);

        // Resolve the raw type
        String resolvedRawType = resolveType(rawType, currentFile);
        if (resolvedRawType == null) {
            return null;
        }

        // Resolve each type parameter
        String[] params = typeParams.split(",");
        List<String> resolvedParams = new ArrayList<>();
        for (String param : params) {
            param = param.trim();
            String resolvedParam = resolveType(param, currentFile);
            resolvedParams.add(resolvedParam != null ? resolvedParam : param);
        }

        // Construct the fully qualified parameterized type
        String resolvedType = resolvedRawType + "<" + String.join(", ", resolvedParams) + ">";
        parameterizedTypeCache.put(type, resolvedType);
        return resolvedType;
    }

    private String resolveTypeFollowingJavaRules(String type, String currentPackage, List<String> imports) {
        // 1. Check same package
        if (packageToTypes.containsKey(currentPackage) &&
                packageToTypes.get(currentPackage).contains(type)) {
            return currentPackage + "." + type;
        }

        // 2. Check explicit imports
        for (String imp : imports) {
            if (imp.endsWith("." + type)) {
                return imp;
            }
        }

        // 3. Check wildcard imports
        for (String imp : imports) {
            if (imp.endsWith(".*")) {
                String packageName = imp.substring(0, imp.length() - 2);
                if (packageToTypes.containsKey(packageName) &&
                        packageToTypes.get(packageName).contains(type)) {
                    return packageName + "." + type;
                }
            }
        }

        // 4. Check java.lang package
        if (isJavaLangType(type)) {
            return "java.lang." + type;
        }

        return null;
    }

    private boolean isJavaLangType(String type) {
        try {
            Class.forName("java.lang." + type);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
