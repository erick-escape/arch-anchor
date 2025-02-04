package tcc.com.viewer.services;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Service;
import tcc.com.viewer.antlr4.JavaLexer;
import tcc.com.viewer.antlr4.JavaParser;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class JavaParserService {
    public JavaParser.CompilationUnitContext parseClass(Path classPath) throws IOException {
        CharStream input = CharStreams.fromPath(classPath);
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        return parser.compilationUnit();
    }
}
