package tcc.com.viewer.services.architecturalAnalyses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tcc.com.viewer.domains.module.Module;

import java.util.List;

@Slf4j
@Service
public class ArchitecturalAnalysesRunner {

	private final List<ArchitecturalAnalysis> analyses;

	public ArchitecturalAnalysesRunner(List<ArchitecturalAnalysis> analyses) {
		this.analyses = analyses;
	}

	public void executeAll(List<Module> modules) {
		log.info("Starting execution of {} analyses", analyses.size());
		analyses.forEach(analysis -> {
			log.info("Executing analysis: {}", analysis.getClass().getSimpleName());
			analysis.execute(modules);
		});
		log.info("Completed execution of all analyses");
	}

}