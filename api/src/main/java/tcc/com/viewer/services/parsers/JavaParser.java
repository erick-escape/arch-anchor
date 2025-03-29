package tcc.com.viewer.services.parsers;

import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.services.JDTParserService;

import java.nio.file.Path;
import java.util.List;

/**
 * Implementation for Java files using JDT
 */
@Component
public class JavaParser implements LanguageParser {

    private final JDTParserService jdtParserService;

    public JavaParser(JDTParserService jdtParserService) {
        this.jdtParserService = jdtParserService;
    }

    @Override
    public boolean canHandle(Path filePath) {
        return filePath.toString().toLowerCase().endsWith(".java");
    }

    @Override
    public List<Dependency> getDependencies(Path filePath) {
        return jdtParserService.getDependencies(filePath);
    }
}