package tcc.com.viewer.services.heuristics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tcc.com.viewer.domains.module.Module;

import java.util.List;

@Slf4j
@Service
public class HeuristicRunner {
    private final List<Heuristic> heuristics;
    
    public HeuristicRunner(List<Heuristic> heuristics) {
        this.heuristics = heuristics;
    }
    
    public void executeAll(List<Module> modules) {
        log.info("Starting execution of {} heuristics", heuristics.size());
        heuristics.forEach(heuristic -> {
            log.info("Executing heuristic: {}", heuristic.getClass().getSimpleName());
            heuristic.execute(modules);
        });
        log.info("Completed execution of all heuristics");
    }
}