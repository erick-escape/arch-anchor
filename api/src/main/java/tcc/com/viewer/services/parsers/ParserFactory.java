package tcc.com.viewer.services.parsers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tcc.com.viewer.services.JDTParserService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating appropriate language parsers based on file extension
 */
@Component
public class ParserFactory {

    private final Map<String, LanguageParser> parsers = new ConcurrentHashMap<>();
    private final JDTParserService jdtParserService = new JDTParserService();

    // Default constructor for Spring
    public ParserFactory() {
    }

    // Constructor with parsers - useful for testing or manual initialization
    public ParserFactory(List<LanguageParser> parsersList) {
        if (parsersList != null) {
            for (LanguageParser parser : parsersList) {
                registerParser(parser);
            }
        }
    }

    @Autowired(required = false)  // Make it optional in case no parsers are available
    public void setParsers(List<LanguageParser> parsersList) {
        if (parsersList != null) {
            for (LanguageParser parser : parsersList) {
                registerParser(parser);
            }
        }
    }

    public void registerParser(LanguageParser parser) {
        // We could be more sophisticated here and register by file extension
        // For now just add to our collection
        parsers.put(parser.getClass().getSimpleName(), parser);
    }

    /**
     * Returns the appropriate parser for the given file
     */
    public LanguageParser getParser(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        String filePathStr = filePath.toString().toLowerCase();

        // Find a parser that can handle this file
        for (LanguageParser parser : parsers.values()) {
            if (parser.canHandle(filePath)) {
                return parser;
            }
        }
        
        if (filePathStr.endsWith(".java")) {
            JavaParser javaParser = new JavaParser(jdtParserService);
            registerParser(javaParser);
            return javaParser;
        }

        throw new UnsupportedOperationException("No parser available for file: " + filePath);
    }
    
    /**
     * Clears the processing cache for fresh analysis
     */
    public void clearProcessingCache() {
        jdtParserService.clearProcessedFilesCache();
    }
}