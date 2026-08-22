package com.archanchor.services.parsers;

import org.springframework.stereotype.Component;
import com.archanchor.domains.dependency.Dependency;

import java.nio.file.Path;
import java.util.List;

/**
 * Implementation for JavaScript/TypeScript files You would implement this with TypeScript
 * Compiler API
 */
@Component
public class JavaScriptParser implements LanguageParser {

	@Override
	public boolean canHandle(Path filePath) {
		String path = filePath.toString().toLowerCase();
		return path.endsWith(".js") || path.endsWith(".ts");
	}

	@Override
	public List<Dependency> getDependencies(Path filePath) {
		// TODO: Implement using TypeScript Compiler API
		throw new UnsupportedOperationException("JavaScript/TypeScript parsing not yet implemented");
	}

}
