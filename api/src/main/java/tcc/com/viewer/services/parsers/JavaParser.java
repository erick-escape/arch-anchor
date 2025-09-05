package tcc.com.viewer.services.parsers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.services.JDTParserService;
import tcc.com.viewer.services.JavaParserService;

import java.nio.file.Path;
import java.util.List;

/**
 * Implementation for Java files that can use either JDT or JavaParser
 * Based on configuration, it will use JavaParserService (default) or fallback to JDTParserService
 */
@Slf4j
@Component
public class JavaParser implements LanguageParser {

    private final JDTParserService jdtParserService;
    private final JavaParserService javaParserService;
    private final boolean useJavaParser;

    public JavaParser(
            JDTParserService jdtParserService,
            @Autowired(required = false) JavaParserService javaParserService,
            @Value("${parser.use-javaparser:true}") boolean useJavaParser) {
        this.jdtParserService = jdtParserService;
        this.javaParserService = javaParserService;
        this.useJavaParser = useJavaParser;
        
        log.info("JavaParser initialized with useJavaParser={}, javaParserService={}", 
                useJavaParser, javaParserService != null ? "available" : "not available");
    }

    @Override
    public boolean canHandle(Path filePath) {
        return filePath.toString().toLowerCase().endsWith(".java");
    }

    @Override
    public List<Dependency> getDependencies(Path filePath) {
        try {
            // Try JavaParserService first if enabled and available
            if (useJavaParser && javaParserService != null) {
                log.debug("Using JavaParserService for: {}", filePath.getFileName());
                return javaParserService.getDependencies(filePath);
            }
        } catch (Exception e) {
            log.warn("JavaParserService failed for {}, falling back to JDT: {}", 
                    filePath.getFileName(), e.getMessage());
        }
        
        // Fallback to JDT parser
        log.debug("Using JDTParserService for: {}", filePath.getFileName());
        return jdtParserService.getDependencies(filePath);
    }
}