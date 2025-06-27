package tcc.com.viewer.services;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Service;
import tcc.com.viewer.antlr4.JavaLexer;
import tcc.com.viewer.antlr4.JavaParser;
import tcc.com.viewer.antlr4.JavaParserBaseVisitor;
import tcc.com.viewer.domains.dependency.Dependency;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class JavaParserService {
    public JavaParser.CompilationUnitContext parseClass(Path classPath) throws IOException {
        CharStream input = CharStreams.fromPath(classPath);
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        return parser.compilationUnit();
    }

    private boolean isNotPrimitiveOrWrapper(String typeName) {
        ArrayList<String> dependenciesToAvoid = new ArrayList<>(List.of(
                "int", "Integer",
                "long", "Long",
                "double", "Double",
                "float", "Float",
                "boolean", "Boolean",
                "char", "Character",
                "byte", "Byte",
                "short", "Short",
                "String"
        ));

        return !dependenciesToAvoid.contains(typeName);
    }

    private void extractDependenciesFromType(JavaParser.TypeTypeContext typeContext, List<Dependency> dependencies) {
        if (typeContext != null) {
            if (typeContext.classOrInterfaceType() != null) {
                JavaParser.ClassOrInterfaceTypeContext classType = typeContext.classOrInterfaceType();
                String typeName = classType.getText();

                // Handle type arguments (e.g., AttendeeDetailsDTO in List<AttendeeDetailsDTO>)
                if (!classType.typeArguments().isEmpty()) {
                    classType.typeArguments().forEach(typeArgumentsContext -> {
                        typeArgumentsContext.typeArgument().forEach(typeArgument -> {
                            if (typeArgument.typeType() != null) {
                                String genericTypeName = typeArgument.typeType().classOrInterfaceType().getText();
                                addDependencyIfValid(genericTypeName, dependencies);
                            }
                        });
                    });
                } else {
                    // If no generics, just add the outer type
                    addDependencyIfValid(typeName, dependencies);
                }
            } else {
                String typeName = typeContext.getText();
                addDependencyIfValid(typeName, dependencies);
            }
        }
    }

    private void addDependencyIfValid(String typeName, List<Dependency> dependencies) {
        if (isNotPrimitiveOrWrapper(typeName)) {
            Dependency dependency = new Dependency(typeName, typeName);
            if (dependency.dependencyDoesNotExist(dependencies)) {
                dependencies.add(dependency);
            }
        }
    }

    public List<Dependency> getDependencies(Path classPath) {
        List<Dependency> dependencies = new ArrayList<>();
        try {
            JavaParser.CompilationUnitContext context = this.parseClass(classPath);

            // Traverse the AST to find dependencies
            context.accept(new JavaParserBaseVisitor<Void>() {
                @Override
                public Void visitImportDeclaration(tcc.com.viewer.antlr4.JavaParser.ImportDeclarationContext ctx) {
                    // Add dependencies from import statements
                    String dependencyName = ctx.qualifiedName().getText();
                    addDependencyIfValid(dependencyName, dependencies);
                    return super.visitImportDeclaration(ctx);
                }

                @Override
                public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
                    // Add dependencies from class `extends` and `implements` clauses
                    if (ctx.typeType() != null) {
                        extractDependenciesFromType(ctx.typeType(), dependencies);
                    }
                    // Implements multiple interfaces
                    if (ctx.typeList() != null) {
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

                                if (member.interfaceMethodDeclaration() != null) {
                                    JavaParser.InterfaceCommonBodyDeclarationContext commonBody = member
                                            .interfaceMethodDeclaration()
                                            .interfaceCommonBodyDeclaration();

                                    // Extract dependencies from return type
                                    if (commonBody.typeTypeOrVoid().typeType() != null) {
                                        extractDependenciesFromType(commonBody.typeTypeOrVoid().typeType(), dependencies);
                                    }

                                    if (commonBody.formalParameters() != null && commonBody.formalParameters().formalParameterList() != null) {
                                        commonBody.formalParameters().formalParameterList().formalParameter().forEach(parameter -> {
                                            extractDependenciesFromType(parameter.typeType(), dependencies);
                                        });
                                    }
                                }

                                if (member.genericInterfaceMethodDeclaration() != null) {
                                    JavaParser.InterfaceCommonBodyDeclarationContext genericMethodCtx = member
                                            .genericInterfaceMethodDeclaration()
                                            .interfaceCommonBodyDeclaration();

                                    // Extract dependencies from return type
                                    if (genericMethodCtx.typeTypeOrVoid().typeType() != null) {
                                        extractDependenciesFromType(genericMethodCtx.typeTypeOrVoid().typeType(), dependencies);
                                    }

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
                    JavaParser.TypeTypeContext returnType = ctx.typeTypeOrVoid().typeType();
                    if (returnType != null) {
                        extractDependenciesFromType(returnType, dependencies);
                    }
                    if (ctx.formalParameters() != null && ctx.formalParameters().formalParameterList() != null) {
                        ctx.formalParameters().formalParameterList().formalParameter().forEach(parameter -> {
                            extractDependenciesFromType(parameter.typeType(), dependencies);
                        });
                    }
                    return super.visitMethodDeclaration(ctx);
                }

                @Override
                public Void visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
                    extractDependenciesFromType(ctx.typeType(), dependencies);
                    return super.visitLocalVariableDeclaration(ctx);
                }

                @Override
                public Void visitObjectCreationExpression(JavaParser.ObjectCreationExpressionContext ctx) {
                    // Add dependencies from `new` operator
                    JavaParser.CreatorContext creator = ctx.creator();
                    if (creator != null && creator.createdName() != null) {
                        String typeName = creator.createdName().getText();
                        JavaParser.NonWildcardTypeArgumentsContext arguments = creator.nonWildcardTypeArguments();
                        if (arguments != null) {
                            Dependency dependency = new Dependency(typeName, typeName);
                            if (isNotPrimitiveOrWrapper(typeName) && dependency.dependencyDoesNotExist(dependencies)) {
                                dependencies.add(dependency);
                            }
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
}
