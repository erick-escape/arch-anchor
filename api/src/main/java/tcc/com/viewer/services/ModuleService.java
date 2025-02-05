package tcc.com.viewer.services;

import org.springframework.stereotype.Service;
import tcc.com.viewer.antlr4.JavaParser;
import tcc.com.viewer.antlr4.JavaParserBaseVisitor;
import tcc.com.viewer.domains.clazz.Clazz;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.domains.module.Module;
import tcc.com.viewer.dto.clazz.ClazzResponseDTO;
import tcc.com.viewer.dto.dependencies.DependencyDTO;
import tcc.com.viewer.dto.module.ModuleDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModuleService {
    private final List<Module> modules = new ArrayList<>();
    private final JavaParserService javaParserService = new JavaParserService();

    private boolean isPrimitiveOrWrapper(String typeName) {
        ArrayList<String> dependenciesToAvoid = new ArrayList<>(List.of("int", "Integer",
                "long", "Long",
                "double", "Double",
                "float", "Float",
                "boolean", "Boolean",
                "char", "Character",
                "byte", "Byte",
                "short", "Short",
                "String"));

        return dependenciesToAvoid.contains(typeName);
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
        } else {
            Clazz clazz = clazzes.get(0);
            clazz.setSimilarity(1.0);
        }
        // Set refClass as the class with the highest similarity
        Clazz refClass = clazzes.stream().max(Comparator.comparingDouble(Clazz::getSimilarity)).orElse(null);
        module.setRefClass(refClass.getName());
    }

    private void calculateModuleSimilarity(Module module) {
        Clazz[] clazzes = module.getClazzes();
        double totalSimilarity = 0.0;
        for (Clazz clazz : clazzes) {
            totalSimilarity += clazz.getSimilarity();
        }
        module.setSimilarity(totalSimilarity / clazzes.length);
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
                Arrays.stream(module.getClazzes())
                        .map(clazz -> new ClazzResponseDTO(clazz.getName(),
                                Arrays.stream(clazz.getDependencies()).map(d -> new DependencyDTO(d.getName())).toArray(DependencyDTO[]::new),
                                clazz.getSimilarity()))
                        .toArray(ClazzResponseDTO[]::new),
                Arrays.stream(module.getDependencies()).map(d -> new DependencyDTO(d.getName())).toArray(DependencyDTO[]::new),
                module.getSimilarity()
        );
    }

    private void extractDependenciesFromType(JavaParser.TypeTypeContext typeContext, List<Dependency> dependencies) {
        if (typeContext != null) {
            // Handle generic types like List<AttendeeDetailsDTO>
            if (typeContext.classOrInterfaceType() != null) {
                JavaParser.ClassOrInterfaceTypeContext classType = typeContext.classOrInterfaceType();
                String typeName = classType.getText(); // Outer type (e.g., List)

                // Handle type arguments (e.g., AttendeeDetailsDTO in List<AttendeeDetailsDTO>)
                if (!classType.typeArguments().isEmpty()) {
                    // Navigate through the type arguments
                    classType.typeArguments().forEach(typeArgumentsContext -> {
                        typeArgumentsContext.typeArgument().forEach(typeArgument -> {
                            if (typeArgument.typeType() != null) {
                                String genericTypeName = typeArgument.typeType().classOrInterfaceType().getText();
                                addDependencyIfValid(genericTypeName, typeArgument.typeType(), dependencies);
                            }
                        });
                    });
                } else {
                    // If no generics, just add the outer type
                    addDependencyIfValid(typeName, typeContext, dependencies);
                }
            } else {
                // Add non-generic type (e.g., String, int)
                String typeName = typeContext.getText();
                addDependencyIfValid(typeName, typeContext, dependencies);
            }
        }
    }

    private void addDependencyIfValid(String typeName, JavaParser.TypeTypeContext typeType, List<Dependency> dependencies) {
        if (!isPrimitiveOrWrapper(typeName)) {
            Dependency dependency = new Dependency(typeName, typeType);
            if (dependency.dependencyDoesNotExist(dependencies)) {
                dependencies.add(dependency);
            }
        }
    }


    private List<Dependency> getDependencies(Path classPath) {
        List<Dependency> dependencies = new ArrayList<>();
        try {
            JavaParser.CompilationUnitContext context = this.javaParserService.parseClass(classPath);

            // Traverse the AST to find dependencies
            context.accept(new JavaParserBaseVisitor<Void>() {
//                @Override
//                public Void visitImportDeclaration(tcc.com.viewer.antlr4.JavaParser.ImportDeclarationContext ctx) {
//                    // Add dependencies from import statements
//                    String dependencyName = ctx.qualifiedName().getText();
//                    dependencies.add(new Dependency(dependencyName));
//                    return null;
//                }

                @Override
                public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
                    // Add dependencies from class `extends` and `implements` clauses
                    if (ctx.typeType() != null) {
                        extractDependenciesFromType(ctx.typeType(), dependencies);
                    }
                    if (ctx.typeList() != null) { // Implements multiple interfaces
                        ctx.typeList().forEach(type -> type.typeType().forEach(typeType -> extractDependenciesFromType(typeType, dependencies)));
                    }
                    return super.visitClassDeclaration(ctx);
                }

                @Override
                public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
                    // Add dependencies from interface `extends` clause
                    if (ctx.typeList() != null) {
                        ctx.typeList().forEach(type -> type.typeType().forEach(typeType -> extractDependenciesFromType(typeType, dependencies)));
                    }

                    // Process interface methods inside interfaceBody
                    if (ctx.interfaceBody() != null) {
                        ctx.interfaceBody().interfaceBodyDeclaration().forEach(bodyDecl -> {
                            if (bodyDecl.interfaceMemberDeclaration() != null) {
                                JavaParser.InterfaceMemberDeclarationContext member = bodyDecl.interfaceMemberDeclaration();

                                // Extract method declarations from the interface
                                if (member.interfaceMethodDeclaration() != null) {
                                    JavaParser.InterfaceCommonBodyDeclarationContext commonBody = member
                                            .interfaceMethodDeclaration()
                                            .interfaceCommonBodyDeclaration();

                                    // Extract dependencies from return type
                                    if (commonBody.typeTypeOrVoid().typeType() != null) {
                                        extractDependenciesFromType(commonBody.typeTypeOrVoid().typeType(), dependencies);
                                    }

                                    // Extract dependencies from method parameters
                                    if (commonBody.formalParameters() != null && commonBody.formalParameters().formalParameterList() != null) {
                                        commonBody.formalParameters().formalParameterList().formalParameter().forEach(parameter -> {
                                            extractDependenciesFromType(parameter.typeType(), dependencies);
                                        });
                                    }
                                }

                                // Extract dependencies from generic interface methods
                                if (member.genericInterfaceMethodDeclaration() != null) {
                                    JavaParser.InterfaceCommonBodyDeclarationContext genericMethodCtx = member
                                            .genericInterfaceMethodDeclaration()
                                            .interfaceCommonBodyDeclaration();

                                    // Extract dependencies from return type
                                    if (genericMethodCtx.typeTypeOrVoid().typeType() != null) {
                                        extractDependenciesFromType(genericMethodCtx.typeTypeOrVoid().typeType(), dependencies);
                                    }

                                    // Extract dependencies from method parameters
                                    if (genericMethodCtx.formalParameters() != null
                                            && genericMethodCtx.formalParameters().formalParameterList() != null) {
                                        genericMethodCtx.formalParameters().formalParameterList().formalParameter().forEach(parameter -> {
                                            extractDependenciesFromType(parameter.typeType(), dependencies);
                                        });
                                    }
                                }
                            }
                        });
                    }

                    return super.visitInterfaceDeclaration(ctx);
                }

                @Override
                public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
                    // Add dependency from `constructor` parameters
                    JavaParser.FormalParametersContext formalParameters = ctx.formalParameters();
                    if (formalParameters != null && formalParameters.formalParameterList() != null) {
                        formalParameters.formalParameterList().formalParameter().forEach(parameter -> {
                            extractDependenciesFromType(parameter.typeType(), dependencies);
                        });
                    }
                    return super.visitConstructorDeclaration(ctx);
                }

                @Override
                public Void visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
                    // Add dependencies from record components
                    if (ctx.recordHeader().recordComponentList() != null) {
                        ctx.recordHeader().recordComponentList().recordComponent().forEach(recordComponent -> {
                            extractDependenciesFromType(recordComponent.typeType(), dependencies);
                        });
                    }
                    return super.visitRecordDeclaration(ctx);
                }

                @Override
                public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
                    // Add dependencies from class attributes
                    extractDependenciesFromType(ctx.typeType(), dependencies);
                    return super.visitFieldDeclaration(ctx);
                }

                @Override
                public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
                    // Add dependencies from method return type and parameters
                    JavaParser.TypeTypeContext returnType = ctx.typeTypeOrVoid().typeType();
                    if (returnType != null) {
                        extractDependenciesFromType(returnType, dependencies);
                    }
                    // Add dependencies from method parameters
                    if (ctx.formalParameters() != null && ctx.formalParameters().formalParameterList() != null) {
                        ctx.formalParameters().formalParameterList().formalParameter().forEach(parameter -> {
                            extractDependenciesFromType(parameter.typeType(), dependencies);
                        });
                    }
                    return super.visitMethodDeclaration(ctx);
                }

                @Override
                public Void visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
                    // Add dependencies from local variable declarations
                    extractDependenciesFromType(ctx.typeType(), dependencies);
                    return super.visitLocalVariableDeclaration(ctx);
                }

                @Override
                public Void visitObjectCreationExpression(JavaParser.ObjectCreationExpressionContext ctx) {
                    // Add dependencies from `new` operator
                    JavaParser.CreatorContext creator = ctx.creator();
                    if (creator != null && creator.createdName() != null) {
//                        String typeName = creator.createdName().getText();
                        JavaParser.NonWildcardTypeArgumentsContext arguments = creator.nonWildcardTypeArguments();
                        if (arguments != null) {
                            arguments.typeList().typeType().forEach(typeType -> extractDependenciesFromType(typeType, dependencies));
//                            Dependency dependency = new Dependency(typeName, );
//                            if (!isPrimitiveOrWrapper(typeName) && dependency.dependencyDoesNotExist(dependencies)) {
//                                dependencies.add(dependency);
//                            }
                        }
                    }
                    return super.visitObjectCreationExpression(ctx);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dependencies;
    }

    private List<Clazz> getClasses(Path modulePath) throws IOException {
        List<Clazz> classes = new ArrayList<>();

        Files.list(modulePath)
                .filter(Files::isRegularFile) // Only regular files
                .filter(file -> file.toString().endsWith(".java")) // Only Java files
                .forEach(javaFilePath -> {
                    Clazz clazz = new Clazz(
                            javaFilePath.getFileName().toString().replace(".java", ""), // Class name
                            getDependencies(javaFilePath).toArray(new Dependency[0]), // Dependencies
                            0.0, // Similarity will be calculated later
                            modulePath.getFileName().toString(), // firstModule
                            modulePath.getFileName().toString() // currentModule
                    );
                    classes.add(clazz);
                });

        return classes;
    }

    public List<Module> getModules(String projectDirectory) throws IOException {
        Path srcPath = Paths.get(projectDirectory, "src"); // Start from 'src' directory
        if (!Files.exists(srcPath) || !Files.isDirectory(srcPath)) {
            return this.modules; // Return empty list if 'src' does not exist or is not a directory
        }

        Files.walk(srcPath) // Only consider directories directly under 'src'
                .filter(Files::isDirectory)
                .forEach(modulePath -> {
                    try {
                        List<Clazz> clazzes = getClasses(modulePath); // Get classes in this directory
                        if (!clazzes.isEmpty()) { // Only include directories with Java files
                            Module module = new Module(
                                    modulePath.getParent().getFileName().toString() +
                                            '/' +
                                            modulePath.getFileName().toString(), // Module name
                                    null, // refClass will be calculated later
                                    clazzes.toArray(new Clazz[0]), // Classes in this module
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
        // Parse project to identify modules and their classes
        List<Module> modules = this.getModules(directoryPath);

        // Calculate similarities for classes and modules
        for (Module module : modules) {
            this.calculateClassSimilarities(module);
            this.calculateModuleSimilarity(module);
        }

        // Convert modules to ModuleDTO
        return modules.stream()
                .map(this::toModuleDTO)
                .collect(Collectors.toList());
    }
}
