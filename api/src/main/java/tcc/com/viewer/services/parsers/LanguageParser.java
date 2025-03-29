package tcc.com.viewer.services.parsers;

import tcc.com.viewer.domains.dependency.Dependency;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface defining the contract for language-specific parsers
 */
public interface LanguageParser {
    /**
     * Checks if this parser can handle the given file based on its extension
     */
    boolean canHandle(Path filePath);

    /**
     * Extracts all dependencies from the file
     */
    List<Dependency> getDependencies(Path filePath);
}