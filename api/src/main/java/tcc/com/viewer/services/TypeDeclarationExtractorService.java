package tcc.com.viewer.services;

import lombok.Getter;
import lombok.Setter;
import tcc.com.viewer.antlr4.JavaParser;
import tcc.com.viewer.antlr4.JavaParserBaseVisitor;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

@Getter
@Setter
public class TypeDeclarationExtractorService extends JavaParserBaseVisitor<Void> {
    private String packageName = "";
    private final Set<String> declaredTypes = new HashSet<>();
    private final Stack<String> currentTypeContext = new Stack<>();

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        packageName = ctx.qualifiedName().getText();
        return null;
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        String className = ctx.identifier().getText();
        addDeclaredType(className);

        // Enter class context for nested types
        currentTypeContext.push(className);
        Void result = super.visitClassDeclaration(ctx);
        currentTypeContext.pop();

        return result;
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        String interfaceName = ctx.identifier().getText();
        addDeclaredType(interfaceName);

        currentTypeContext.push(interfaceName);
        Void result = super.visitInterfaceDeclaration(ctx);
        currentTypeContext.pop();

        return result;
    }

    @Override
    public Void visitEnumDeclaration(JavaParser.EnumDeclarationContext ctx) {
        String enumName = ctx.identifier().getText();
        addDeclaredType(enumName);

        currentTypeContext.push(enumName);
        Void result = super.visitEnumDeclaration(ctx);
        currentTypeContext.pop();

        return result;
    }

    @Override
    public Void visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
        String recordName = ctx.identifier().getText();
        addDeclaredType(recordName);

        currentTypeContext.push(recordName);
        Void result = super.visitRecordDeclaration(ctx);
        currentTypeContext.pop();

        return result;
    }

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
}
