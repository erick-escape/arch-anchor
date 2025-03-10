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
    private final List<String> explicitImports = new ArrayList<>();
    private final Set<String> wildcardImports = new HashSet<>();
    private final Set<String> usedTypes = new HashSet<>();
    private final Stack<String> currentTypeContext = new Stack<>();

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
        String importPath = ctx.qualifiedName().getText();

        // Handle static imports differently
        if (ctx.STATIC() != null) {
            // For simplicity, we're not resolving static imports here
            return null;
        }

        // Handle wildcard imports
        if (ctx.getText().endsWith(".*;")) {
            wildcardImports.add(importPath + ".*");
        } else {
            // Regular explicit import
            explicitImports.add(importPath);
        }

        return null;
    }

    // All the visitor methods for types will call this
    private void trackUsedType(String typeName) {
        // Ignore primitive types, null, etc.
        if (typeName == null || typeName.isEmpty() ||
                isPrimitiveType(typeName) || isJavaKeyword(typeName)) {
            return;
        }

        // Only add simple names (nonqualified)
        if (!typeName.contains(".")) {
            usedTypes.add(typeName);
        }
    }

    private boolean isPrimitiveType(String typeName) {
        return typeName.equals("boolean") || typeName.equals("byte") ||
                typeName.equals("char") || typeName.equals("short") ||
                typeName.equals("int") || typeName.equals("long") ||
                typeName.equals("float") || typeName.equals("double") ||
                typeName.equals("void");
    }

    private boolean isJavaKeyword(String typeName) {
        // This is not comprehensive but includes many common keywords
        return typeName.equals("this") || typeName.equals("super") ||
                typeName.equals("null") || typeName.equals("true") ||
                typeName.equals("false") || typeName.equals("new");
    }

    // Classes, interfaces, enums, records
    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        // Handle "extends" clause
        if (ctx.EXTENDS() != null && ctx.typeType() != null) {
            processTypeType(ctx.typeType());
        }

        // Handle "implements" clause
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            for (JavaParser.TypeListContext typeList : ctx.typeList()) {
                processTypeList(typeList);
            }
        }

        // Handle "permits" clause
        if (ctx.PERMITS() != null && ctx.typeList(1) != null) {
            processTypeList(ctx.typeList(1));
        }

        return super.visitClassDeclaration(ctx);
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        // Handle "extends" clause
        if (ctx.EXTENDS() != null && ctx.typeList() != null) {
            for (JavaParser.TypeListContext typeList : ctx.typeList()) {
                processTypeList(typeList);
            }
        }

        return super.visitInterfaceDeclaration(ctx);
    }

    @Override
    public Void visitEnumDeclaration(JavaParser.EnumDeclarationContext ctx) {
        // Handle "implements" clause
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            processTypeList(ctx.typeList());
        }

        return super.visitEnumDeclaration(ctx);
    }

    @Override
    public Void visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
        // Process record components
        if (ctx.recordHeader() != null &&
                ctx.recordHeader().recordComponentList() != null) {
            for (JavaParser.RecordComponentContext component :
                    ctx.recordHeader().recordComponentList().recordComponent()) {
                processTypeType(component.typeType());
            }
        }

        // Handle "implements" clause
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            processTypeList(ctx.typeList());
        }

        return super.visitRecordDeclaration(ctx);
    }

    // Method and constructor declarations
    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        // Process return type
        if (ctx.typeTypeOrVoid() != null && ctx.typeTypeOrVoid().typeType() != null) {
            processTypeType(ctx.typeTypeOrVoid().typeType());
        }

        // Process exceptions
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            for (JavaParser.QualifiedNameContext name : ctx.qualifiedNameList().qualifiedName()) {
                trackUsedType(name.getText());
            }
        }

        return super.visitMethodDeclaration(ctx);
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        // Process exceptions
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            for (JavaParser.QualifiedNameContext name : ctx.qualifiedNameList().qualifiedName()) {
                trackUsedType(name.getText());
            }
        }

        return super.visitConstructorDeclaration(ctx);
    }

    // Variables, fields, and parameters
    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        processTypeType(ctx.typeType());
        return super.visitFieldDeclaration(ctx);
    }

    @Override
    public Void visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        if (ctx.typeType() != null) {
            processTypeType(ctx.typeType());
        }
        return super.visitLocalVariableDeclaration(ctx);
    }

    @Override
    public Void visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        processTypeType(ctx.typeType());
        return super.visitFormalParameter(ctx);
    }

    // Handle class or interface types
    @Override
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        // Track the type identifier (the class name)
        if (ctx.typeIdentifier() != null) {
            trackUsedType(ctx.typeIdentifier().getText());
        }

        // Track each identifier in the qualified name
        for (JavaParser.IdentifierContext id : ctx.identifier()) {
            trackUsedType(id.getText());
        }

        // Process type arguments
        for (JavaParser.TypeArgumentsContext args : ctx.typeArguments()) {
            for (JavaParser.TypeArgumentContext arg : args.typeArgument()) {
                if (arg.typeType() != null) {
                    processTypeType(arg.typeType());
                }
            }
        }

        return null;
    }

    // Handle expressions that may reference types
    @Override
    public Void visitCastExpression(JavaParser.CastExpressionContext ctx) {
        for (JavaParser.TypeTypeContext type : ctx.typeType()) {
            processTypeType(type);
        }
        return super.visitCastExpression(ctx);
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        if (ctx.createdName() != null) {
            for (JavaParser.IdentifierContext id : ctx.createdName().identifier()) {
                trackUsedType(id.getText());
            }
        }
        return super.visitCreator(ctx);
    }

    @Override
    public Void visitCatchClause(JavaParser.CatchClauseContext ctx) {
        if (ctx.catchType() != null) {
            for (JavaParser.QualifiedNameContext name : ctx.catchType().qualifiedName()) {
                trackUsedType(name.getText());
            }
        }
        return super.visitCatchClause(ctx);
    }

    @Override
    public Void visitTypeArgument(JavaParser.TypeArgumentContext ctx) {
        if (ctx.typeType() != null) {
            processTypeType(ctx.typeType());
        }
        return null;
    }

//    @Override
//    public Void visitAnnotation(JavaParser.AnnotationContext ctx) {
//        // Extract the annotation type name
//        if (ctx.qualifiedName() != null) {
//            String annotationName = ctx.qualifiedName().getText();
//            // Handle qualified names - extract just the class name
//            if (annotationName.contains(".")) {
//                String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);
//                trackUsedType(simpleName);
//            } else {
//                trackUsedType(annotationName);
//            }
//        }
//
//        // Visit any arguments (might contain type references)
//        if (ctx.elementValuePairs() != null) {
//            return visitElementValuePairs(ctx.elementValuePairs());
//        } else if (ctx.elementValue() != null) {
//            return visitElementValue(ctx.elementValue());
//        }
//
//        return null;
//    }

    // Helper methods to process complex type structures
    private void processTypeType(JavaParser.TypeTypeContext ctx) {
        if (ctx == null) return;

        if (ctx.classOrInterfaceType() != null) {
            visitClassOrInterfaceType(ctx.classOrInterfaceType());
        }
        // Primitives don't need type resolution
    }

    private void processTypeList(JavaParser.TypeListContext ctx) {
        if (ctx == null) return;

        for (JavaParser.TypeTypeContext type : ctx.typeType()) {
            processTypeType(type);
        }
    }
}
