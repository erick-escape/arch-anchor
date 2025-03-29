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
import java.util.stream.Collectors;

public class TypeResolverService {
    private final Map<String, String> typeResolutions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> packageToTypes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> fileImports = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> fileWildcardImports = new ConcurrentHashMap<>();
    private final Map<String, String> filePackages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> fileUsedTypes = new ConcurrentHashMap<>();

    // Cache for nested types (format: "OuterClass$InnerClass" -> "fully.qualified.OuterClass$InnerClass")
    private final Map<String, String> nestedTypeCache = new ConcurrentHashMap<>();

    // Cache for parameterized types (format: "List<String>" -> "java.util.List<java.lang.String>")
    private final Map<String, String> parameterizedTypeCache = new ConcurrentHashMap<>();

    /**
     * Scans the entire project directory to build the type resolution cache
     */
    public void scanProject(Path projectRoot) throws IOException {
        // First pass: collect all declared types in the project
        collectAllTypes(projectRoot);

        // Second pass: analyze type usage in each file
        analyzeTypeUsage(projectRoot);

        // Resolve nested types and type parameters
        resolveNestedAndParameterizedTypes();
    }

    private void collectAllTypes(Path projectRoot) throws IOException {
        Files.walk(projectRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(this::collectTypesFromFile);
    }

    private void collectTypesFromFile(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            JavaLexer lexer = new JavaLexer(CharStreams.fromString(content));
            JavaParser parser = new JavaParser(new CommonTokenStream(lexer));
            ParseTree tree = parser.compilationUnit();

            // Create a visitor to extract type declarations
            TypeDeclarationExtractorService visitor = new TypeDeclarationExtractorService();
            visitor.visit(tree);

            String packageName = visitor.getPackageName();
            filePackages.put(javaFile.toString(), packageName);

            // Add types to package mapping
            Set<String> typesInPackage = packageToTypes.computeIfAbsent(
                    packageName, k -> new HashSet<>());
            typesInPackage.addAll(visitor.getDeclaredTypes());
        } catch (IOException e) {
            System.out.println("collectTypesFromFile method: " + e.getMessage());
        }
    }

    private void analyzeTypeUsage(Path projectRoot) throws IOException {
        Files.walk(projectRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(this::analyzeFileTypeUsage);
    }

    private void analyzeFileTypeUsage(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            JavaLexer lexer = new JavaLexer(CharStreams.fromString(content));
            JavaParser parser = new JavaParser(new CommonTokenStream(lexer));
            ParseTree tree = parser.compilationUnit();

            // Now create a visitor that analyzes import statements and type usage
            TypeExtractorVisitorService visitor = new TypeExtractorVisitorService(javaFile);
            visitor.visit(tree);

            // Store the analyzed information
            fileImports.put(javaFile.toString(), visitor.getExplicitImports());
            fileWildcardImports.put(javaFile.toString(), visitor.getWildcardImports());
            fileUsedTypes.put(javaFile.toString(), visitor.getUsedTypes());

            // Update global type resolution map with explicit imports
            for (String importedType : visitor.getExplicitImports()) {
                if (importedType.lastIndexOf('.') != -1) {
                    String simpleName = importedType.substring(importedType.lastIndexOf('.') + 1);
                    typeResolutions.put(simpleName, importedType);
                }
            }

            // Resolve used types that come from wildcard imports
            resolveWildcardImports(javaFile.toString(), visitor);

        } catch (IOException e) {
            System.out.println("analyzeFileTypeUsage method: " + e.getMessage());
        }
    }

    private void resolveWildcardImports(String filePath, TypeExtractorVisitorService visitor) {
        String packageName = filePackages.get(filePath);
        Set<String> usedTypes = visitor.getUsedTypes();
        Set<String> wildcardImports = visitor.getWildcardImports();
        Map<String, String> resolvedWildcardTypes = new HashMap<>();

        // For each used type, try to resolve it
        for (String typeName : usedTypes) {
            // Skip fully qualified names
            if (typeName.contains(".")) continue;

            // Handle already resolved types differently
            if (typeResolutions.containsKey(typeName)) {
                String resolvedType = typeResolutions.get(typeName);

                // Check if this resolved type could have come from any of our wildcard imports
                for (String wildcardImport : wildcardImports) {
                    String packagePath = wildcardImport.substring(0, wildcardImport.length() - 2); // Remove ".*"

                    if (resolvedType.startsWith(packagePath + ".")) {
                        // This type could have come from this wildcard import
                        resolvedWildcardTypes.put(typeName, resolvedType);
                        break;
                    }
                }

                // Continue to next type since we've already handled this one
                continue;
            }

            // First check if it's from the same package
            if (packageToTypes.containsKey(packageName) &&
                    packageToTypes.get(packageName).contains(typeName)) {
                resolvedWildcardTypes.put(typeName, packageName + "." + typeName);
                continue;
            }

            // Check all wildcard imports
            for (String wildcardImport : wildcardImports) {
                String packagePath = wildcardImport.substring(0, wildcardImport.length() - 2); // Remove ".*"

                if (packageToTypes.containsKey(packagePath) &&
                        packageToTypes.get(packagePath).contains(typeName)) {
                    resolvedWildcardTypes.put(typeName, packagePath + "." + typeName);
                    break;
                }
            }

            // Check java.lang package (implicit import)
            if (!resolvedWildcardTypes.containsKey(typeName)) {
                if (isJavaLangType(typeName)) {
                    resolvedWildcardTypes.put(typeName, "java.lang." + typeName);
                }
            }
        }

        // Add the resolved wildcard types to the explicit imports for this file
        List<String> currentImports = fileImports.getOrDefault(filePath, new ArrayList<>());
        currentImports.addAll(resolvedWildcardTypes.values());
        fileImports.put(filePath, currentImports);

        // Also update the global type resolutions
        typeResolutions.putAll(resolvedWildcardTypes);
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
     * Gets the expanded imports for a file, including resolved wildcard imports
     */
    public List<String> getExpandedImports(String filePath) {
        List<String> imports = fileImports.getOrDefault(filePath, new ArrayList<>());

        // Filter to keep only unique imports and sort them
        return imports.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Resolves a type, handling complex cases like nested types and generics
     */
    public String resolveType(String type, String currentFile) {
        // Skip already qualified types
        if (type.contains(".")) {
            return type;
        }

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

        // Get current file's package
        String currentPackage = filePackages.get(currentFile);

        // Try to resolve using the expanded imports for this file
        List<String> expandedImports = getExpandedImports(currentFile);
        for (String imp : expandedImports) {
            if (imp.endsWith("." + type)) {
                typeResolutions.put(type, imp);
                return imp;
            }
        }

        // If still not found, check if it's in the current package
        if (packageToTypes.containsKey(currentPackage) &&
                packageToTypes.get(currentPackage).contains(type)) {
            resolved = currentPackage + "." + type;
            typeResolutions.put(type, resolved);
            return resolved;
        }

        // Final check for java.lang package
        if (isJavaLangType(type)) {
            resolved = "java.lang." + type;
            typeResolutions.put(type, resolved);
            return resolved;
        }

        return null;
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

    private boolean isJavaLangType(String type) {
        try {
            Class.forName("java.lang." + type);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
