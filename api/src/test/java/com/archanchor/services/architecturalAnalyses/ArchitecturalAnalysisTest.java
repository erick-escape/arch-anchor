package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.module.Module;
import com.archanchor.services.ModuleService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ArchitecturalAnalysis#simulateOn}, the guard every analysis wraps its
 * what-if computations in (#16).
 */
class ArchitecturalAnalysisTest {

	private final SimulationProbe analysis = new SimulationProbe();

	@Test
	void simulateOnReturnsWhatTheSimulationComputed() {
		Module web = moduleWith(clazzWithSimilarity("OrderController", 0.8));

		double rate = analysis.simulate(List.of(web), () -> 0.42);

		assertThat(rate).isEqualTo(0.42);
	}

	@Test
	void simulateOnRestoresTheMetricsTheSimulationOverwrote() {
		Clazz controller = clazzWithSimilarity("OrderController", 0.8);
		Module web = moduleWith(controller);

		analysis.simulate(List.of(web), () -> {
			controller.setSimilarity(1.0);
			return 0.42;
		});

		assertThat(controller.getSimilarity()).isEqualTo(0.8);
	}

	@Test
	void simulateOnRestoresTheMetricsEvenWhenTheSimulationFails() {
		Clazz controller = clazzWithSimilarity("OrderController", 0.8);
		Module web = moduleWith(controller);

		assertThatThrownBy(() -> analysis.simulate(List.of(web), () -> {
			controller.setSimilarity(1.0);
			throw new IllegalStateException("simulation failed");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(controller.getSimilarity()).isEqualTo(0.8);
	}

	private static Clazz clazzWithSimilarity(String name, double similarity) {
		return new Clazz(name, name, List.of(), similarity, 1.0, "shop/web", "shop/web", "ALLOW");
	}

	private static Module moduleWith(Clazz... clazzes) {
		return new Module("shop/web", "shop/web", null, null, null, new ArrayList<>(List.of(clazzes)), 0.0, 0.0, 0);
	}

	/** Exposes the protected guard; it has no analysis of its own. */
	private static final class SimulationProbe extends ArchitecturalAnalysis {

		SimulationProbe() {
			super(new ModuleService(null));
		}

		@Override
		public void execute(List<Module> modules) {
		}

		<T> T simulate(List<Module> realModules, Supplier<T> whatIf) {
			return simulateOn(realModules, whatIf);
		}

	}

}
