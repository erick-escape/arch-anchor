package tcc.com.viewer.services;

import lombok.Getter;
import lombok.Setter;
import tcc.com.viewer.antlr4.JavaParser;
import tcc.com.viewer.antlr4.JavaParserBaseVisitor;

import java.nio.file.Path;
import java.util.*;

@Getter
@Setter
public class TypeExtractorVisitorService extends JavaParserBaseVisitor<Void> {
    private final Path sourceFile;
    private String packageName = "";
    private final List<String> imports = new ArrayList<>();
    private final Set<String> declaredTypes = new HashSet<>();
    private final Stack<String> currentTypeContext = new Stack<>();
    private final Set<String> usedTypes = new HashSet<>();

    public TypeExtractorVisitorService(Path sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        packageName = ctx.qualifiedName().getText();
        return super.visitPackageDeclaration(ctx);
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        imports.add(ctx.qualifiedName().getText());
        return super.visitImportDeclaration(ctx);
    }

    // Class, interface, enum, and record declarations
    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        String className = ctx.identifier().getText();
        addDeclaredType(className);

        // Enter class context for nested types
        currentTypeContext.push(className);

        // Handle "extends" clause
        if (ctx.EXTENDS() != null && ctx.typeType() != null) {
            addUsedType(ctx.typeType());
        }

        // Handle "implements" clause
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            for (JavaParser.TypeListContext typeListContext : ctx.typeList()) {
                addUsedType(typeListContext);
            }
        }

        // Handle "permits" clause (for sealed classes)
        if (ctx.PERMITS() != null && ctx.typeList(1) != null) {
            addUsedType(ctx.typeList(1));
        }

        Void result = super.visitClassDeclaration(ctx);

        // Exit class context
        currentTypeContext.pop();

        return result;
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        String interfaceName = ctx.identifier().getText();
        addDeclaredType(interfaceName);

        // Enter interface context for nested types
        currentTypeContext.push(interfaceName);

        // Handle "extends" clause
        if (ctx.EXTENDS() != null && ctx.typeList() != null) {
            for (JavaParser.TypeListContext typeListContext : ctx.typeList()) {
                addUsedType(typeListContext);
            }
        }

        // Handle "permits" clause (for sealed interfaces)
        if (ctx.PERMITS() != null && ctx.typeList(1) != null) {
            addUsedType(ctx.typeList(1));
        }

        Void result = super.visitInterfaceDeclaration(ctx);

        // Exit interface context
        currentTypeContext.pop();

        return result;
    }

    @Override
    public Void visitEnumDeclaration(JavaParser.EnumDeclarationContext ctx) {
        String enumName = ctx.identifier().getText();
        addDeclaredType(enumName);

        // Enter enum context for nested types
        currentTypeContext.push(enumName);

        // Handle "implements" clause
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            addUsedType(ctx.typeList());
        }

        Void result = super.visitEnumDeclaration(ctx);

        // Exit enum context
        currentTypeContext.pop();

        return result;
    }

    @Override
    public Void visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
        String recordName = ctx.identifier().getText();
        addDeclaredType(recordName);

        // Enter record context for nested types
        currentTypeContext.push(recordName);

        // Process record components
        if (ctx.recordHeader() != null &&
                ctx.recordHeader().recordComponentList() != null) {
            for (JavaParser.RecordComponentContext component :
                    ctx.recordHeader().recordComponentList().recordComponent()) {
                addUsedType(component.typeType());
            }
        }

        // Handle "implements" clause
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            addUsedType(ctx.typeList());
        }

        Void result = super.visitRecordDeclaration(ctx);

        // Exit record context
        currentTypeContext.pop();

        return result;
    }

    // Method declarations
    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        // Process return type
        if (ctx.typeTypeOrVoid() != null) {
            if (ctx.typeTypeOrVoid().typeType() != null) {
                addUsedType(ctx.typeTypeOrVoid().typeType());
            }
        }

        // Process parameter types in the method signature
        if (ctx.formalParameters() != null) {
            visitFormalParameters(ctx.formalParameters());
        }

        // Process exceptions thrown by the method
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            for (JavaParser.QualifiedNameContext name : ctx.qualifiedNameList().qualifiedName()) {
                addUsedType(name.getText());
            }
        }

        return super.visitMethodDeclaration(ctx);
    }

    @Override
    public Void visitInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        // Process return type for interface methods
        if (ctx.typeTypeOrVoid() != null) {
            if (ctx.typeTypeOrVoid().typeType() != null) {
                addUsedType(ctx.typeTypeOrVoid().typeType());
            }
        }

        // Process parameter types
        if (ctx.formalParameters() != null) {
            visitFormalParameters(ctx.formalParameters());
        }

        // Process exceptions
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            for (JavaParser.QualifiedNameContext name : ctx.qualifiedNameList().qualifiedName()) {
                addUsedType(name.getText());
            }
        }

        return super.visitInterfaceCommonBodyDeclaration(ctx);
    }

    // Constructor declarations
    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        // Process parameter types
        if (ctx.formalParameters() != null) {
            visitFormalParameters(ctx.formalParameters());
        }

        // Process exceptions
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            for (JavaParser.QualifiedNameContext name : ctx.qualifiedNameList().qualifiedName()) {
                addUsedType(name.getText());
            }
        }

        return super.visitConstructorDeclaration(ctx);
    }

    // Field declarations
    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        if (ctx.typeType() != null) {
            addUsedType(ctx.typeType());
        }
        return super.visitFieldDeclaration(ctx);
    }

    // Local variable declarations
    @Override
    public Void visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        if (ctx.typeType() != null) {
            addUsedType(ctx.typeType());
        }
        return super.visitLocalVariableDeclaration(ctx);
    }

    // Formal parameters
    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        // Process receiver parameter
        if (ctx.receiverParameter() != null) {
            addUsedType(ctx.receiverParameter().typeType());
        }

        // Process formal parameter list
        if (ctx.formalParameterList() != null) {
            return visitFormalParameterList(ctx.formalParameterList());
        }

        return null;
    }

    @Override
    public Void visitFormalParameterList(JavaParser.FormalParameterListContext ctx) {
        // Process regular parameters
        for (JavaParser.FormalParameterContext param : ctx.formalParameter()) {
            addUsedType(param.typeType());
        }

        // Process varargs parameter
        if (ctx.lastFormalParameter() != null) {
            addUsedType(ctx.lastFormalParameter().typeType());
        }

        return null;
    }

    // Catch clauses
    @Override
    public Void visitCatchClause(JavaParser.CatchClauseContext ctx) {
        if (ctx.catchType() != null) {
            for (JavaParser.QualifiedNameContext name : ctx.catchType().qualifiedName()) {
                addUsedType(name.getText());
            }
        }
        return super.visitCatchClause(ctx);
    }

    // Type parameters and bounds
    @Override
    public Void visitTypeParameter(JavaParser.TypeParameterContext ctx) {
        if (ctx.typeBound() != null) {
            for (JavaParser.TypeTypeContext type : ctx.typeBound().typeType()) {
                addUsedType(type);
            }
        }
        return super.visitTypeParameter(ctx);
    }

    // Generic method invocations
    @Override
    public Void visitTypeArguments(JavaParser.TypeArgumentsContext ctx) {
        for (JavaParser.TypeArgumentContext arg : ctx.typeArgument()) {
            if (arg.typeType() != null) {
                addUsedType(arg.typeType());
            }
        }
        return super.visitTypeArguments(ctx);
    }

    // Cast expressions
    @Override
    public Void visitCastExpression(JavaParser.CastExpressionContext ctx) {
        for (JavaParser.TypeTypeContext type : ctx.typeType()) {
            addUsedType(type);
        }
        return super.visitCastExpression(ctx);
    }

    // Object creation expressions
    @Override
    public Void visitObjectCreationExpression(JavaParser.ObjectCreationExpressionContext ctx) {
        if (ctx.creator() != null) {
            // Handle created type
            if (ctx.creator().createdName() != null) {
                JavaParser.CreatedNameContext createdName = ctx.creator().createdName();

                // For class types
                for (int i = 0; i < createdName.identifier().size(); i++) {
                    String identifier = createdName.identifier(i).getText();
                    addUsedType(identifier);

                    // Handle type arguments if present
                    if (createdName.typeArgumentsOrDiamond(i) != null &&
                            createdName.typeArgumentsOrDiamond(i).typeArguments() != null) {
                        visitTypeArguments(createdName.typeArgumentsOrDiamond(i).typeArguments());
                    }
                }
            }
        }
        return super.visitObjectCreationExpression(ctx);
    }

    // Instance of expressions
    @Override
    public Void visitInstanceOfOperatorExpression(JavaParser.InstanceOfOperatorExpressionContext ctx) {
        if (ctx.typeType() != null) {
            addUsedType(ctx.typeType());
        }
        return super.visitInstanceOfOperatorExpression(ctx);
    }

    // Handle type references in method references
    @Override
    public Void visitMethodReferenceExpression(JavaParser.MethodReferenceExpressionContext ctx) {
        if (ctx.typeType() != null) {
            addUsedType(ctx.typeType());
        }
        if (ctx.classType() != null) {
            // Process class type in method reference
            addUsedType(ctx.classType().getText());
        }
        return super.visitMethodReferenceExpression(ctx);
    }

    // Handle lambda parameter types
    @Override
    public Void visitLambdaParameters(JavaParser.LambdaParametersContext ctx) {
        if (ctx.formalParameterList() != null) {
            visitFormalParameterList(ctx.formalParameterList());
        }
        return super.visitLambdaParameters(ctx);
    }

    // Helper methods for type handling
    private void addDeclaredType(String typeName) {
        if (currentTypeContext.isEmpty()) {
            // Top-level type
            declaredTypes.add(typeName);
        } else {
            // Nested type
            String outerType = String.join("$", currentTypeContext);
            declaredTypes.add(outerType + "$" + typeName);
        }
    }

    private void addUsedType(JavaParser.TypeTypeContext ctx) {
        if (ctx == null) return;

        if (ctx.classOrInterfaceType() != null) {
            // Handle class or interface type
            JavaParser.ClassOrInterfaceTypeContext classType = ctx.classOrInterfaceType();

            // Get the whole type name including all parts
            StringBuilder typeName = new StringBuilder();

            // Process identifier parts
            for (int i = 0; i < classType.identifier().size(); i++) {
                if (i > 0) typeName.append(".");
                typeName.append(classType.identifier(i).getText());
            }

            // Add the type identifier (the last part)
            if (classType.typeIdentifier() != null) {
                if (!typeName.isEmpty()) typeName.append(".");
                typeName.append(classType.typeIdentifier().getText());
            }

            addUsedType(typeName.toString());

            // Process type arguments if present
            if (classType.typeArguments() != null) {
                for (JavaParser.TypeArgumentsContext typeArgumentsContext : classType.typeArguments()) {
                    visitTypeArguments(typeArgumentsContext);
                }
            }

            // For each nested part that has type arguments
            for (int i = 0; i < classType.typeArguments().size(); i++) {
                visitTypeArguments(classType.typeArguments(i));
            }
        } else if (ctx.primitiveType() != null) {
            // Handle primitive type (no resolution needed)
            // But we might want to track it for completeness
            String primitiveName = ctx.primitiveType().getText();
            // No need to add to usedTypes as primitives don't need resolution
        }

        // Handle array types - they have the same base type
        // No extra processing needed as we've already processed the base type
    }

    private void addUsedType(JavaParser.TypeListContext ctx) {
        if (ctx == null) return;

        for (JavaParser.TypeTypeContext type : ctx.typeType()) {
            addUsedType(type);
        }
    }

    private void addUsedType(String typeName) {
        usedTypes.add(typeName);
    }
}
