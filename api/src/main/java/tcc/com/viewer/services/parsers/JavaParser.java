package tcc.com.viewer.services.parsers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.dependency.Dependency;
import tcc.com.viewer.services.JavaParserService;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Implementation for Java files that can use either JDT or JavaParser
 * Based on configuration, it will use JavaParserService (default) or fallback to JDTParserService
 */
@Slf4j
@Component
public class JavaParser implements LanguageParser {
    private final JavaParserService javaParserService;

    public JavaParser(@Autowired(required = false) JavaParserService javaParserService) {
        this.javaParserService = javaParserService;

        log.info("JavaParser initialized");
    }

    @Override
    public boolean canHandle(Path filePath) {
        return filePath.toString().toLowerCase().endsWith(".java");
    }

    @Override
    public List<Dependency> getDependencies(Path filePath) {
        try {
            if (javaParserService != null) {
                log.debug("Using JavaParserService for: {}", filePath.getFileName());
                return javaParserService.getDependencies(filePath);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("JavaParserService failed for {}, falling back to JDT: {}",
                    filePath.getFileName(), e.getMessage());

            return Collections.emptyList();
        }
    }
}