package com.archanchor.services;

import com.archanchor.domains.dependency.Dependency;
import com.archanchor.services.parsers.JavaDependencyReader;
import com.archanchor.services.parsers.JavaParser;
import com.archanchor.services.parsers.LanguageParsers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in for the reader that resolves a Java file's types: it reports the same
 * dependencies for every file without reading it, so that how a project's sources become
 * modules can be tested on empty source files.
 *
 * <pre>
 * ModuleService moduleService = new ModuleService(UniformJavaDependencies.parsersReporting("java.sql"), new ActiveRatingWeights());
 * </pre>
 */
final class UniformJavaDependencies implements JavaDependencyReader {

	private final List<String> packageNames;

	private UniformJavaDependencies(String... packageNames) {
		this.packageNames = List.of(packageNames);
	}

	/** The tool's parsers, with every Java file depending on these packages. */
	static LanguageParsers parsersReporting(String... packageNames) {
		return new LanguageParsers(List.of(new JavaParser(new UniformJavaDependencies(packageNames))));
	}

	// A fresh list each time: the analyses change a class's dependencies in place.
	@Override
	public List<Dependency> getDependencies(Path javaSource) {
		List<Dependency> dependencies = new ArrayList<>();
		packageNames.forEach(packageName -> dependencies.add(new Dependency(packageName)));
		return dependencies;
	}

	@Override
	public void clearCache() {
		// Nothing is resolved, so there is nothing to forget.
	}

}
