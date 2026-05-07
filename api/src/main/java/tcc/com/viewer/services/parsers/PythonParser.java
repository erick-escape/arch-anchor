package tcc.com.viewer.services.parsers;

import org.springframework.stereotype.Component;
import tcc.com.viewer.domains.dependency.Dependency;

import java.nio.file.Path;
import java.util.List;

/**
 * Implementation for Python files You would implement this with Jedi or Pyright
 */
@Component
public class PythonParser implements LanguageParser {

	@Override
	public boolean canHandle(Path filePath) {
		return filePath.toString().endsWith(".py");
	}

	@Override
	public List<Dependency> getDependencies(Path filePath) {
		// TODO: Implement using Jedi or Pyright
		throw new UnsupportedOperationException("Python parsing not yet implemented");
	}

}
