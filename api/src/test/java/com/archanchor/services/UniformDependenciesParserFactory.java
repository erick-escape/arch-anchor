package com.archanchor.services;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.services.parsers.LanguageParser;
import com.archanchor.services.parsers.ParserFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in for the language parsers: it reports the same dependencies for every file
 * without reading it, so module discovery can be tested on empty source files.
 *
 * <pre>
 * ModuleService moduleService = new ModuleService(new UniformDependenciesParserFactory("java.sql"));
 * </pre>
 */
final class UniformDependenciesParserFactory extends ParserFactory {

	private final List<String> packageNames;

	UniformDependenciesParserFactory(String... packageNames) {
		super(null);
		this.packageNames = List.of(packageNames);
	}

	@Override
	public LanguageParser getParser(Path filePath) {
		return new UniformDependenciesParser();
	}

	private final class UniformDependenciesParser implements LanguageParser {

		@Override
		public boolean canHandle(Path filePath) {
			return true;
		}

		@Override
		public List<Dependency> getDependencies(Path filePath) {
			List<Dependency> dependencies = new ArrayList<>();
			packageNames.forEach(packageName -> dependencies.add(new Dependency(packageName)));
			return dependencies;
		}

	}

}
