package com.archanchor.controllers;

import com.archanchor.domains.module.Module;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysesRunner;
import com.archanchor.services.architecturalAnalyses.ArchitecturalAnalysis;

import java.util.List;

/**
 * Runs the analyses like {@link ArchitecturalAnalysesRunner} and counts the runs, so a
 * test can tell whether a request refreshed the recommendations, and how often (#28).
 *
 * <pre>
 * CountingAnalysesRunner runner = new CountingAnalysesRunner(List.of(splitModule));
 * runner.executeAll(modules);
 * runner.runs(); // 1
 * </pre>
 */
final class CountingAnalysesRunner extends ArchitecturalAnalysesRunner {

	private int runs;

	CountingAnalysesRunner(List<ArchitecturalAnalysis> analyses) {
		super(analyses);
	}

	@Override
	public void executeAll(List<Module> modules) {
		runs++;
		super.executeAll(modules);
	}

	int runs() {
		return runs;
	}

}
