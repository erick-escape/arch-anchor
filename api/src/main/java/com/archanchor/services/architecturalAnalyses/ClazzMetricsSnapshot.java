package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.clazz.Clazz;

import java.util.Collection;
import java.util.List;

/**
 * The similarity metrics of a set of classes at one moment, so that a what-if computation
 * can be undone. Hypothetical modules reuse the real {@link Clazz} instances, and
 * {@code ModuleService} stores what it computes in them (#16).
 *
 * <pre>
 * ClazzMetricsSnapshot asAnalysed = ClazzMetricsSnapshot.of(module.getClazzes());
 * moduleService.calculateClassSimilarities(hypotheticalModule);
 * asAnalysed.restore();
 * </pre>
 */
final class ClazzMetricsSnapshot {

	private final List<RecordedMetrics> recorded;

	private ClazzMetricsSnapshot(List<RecordedMetrics> recorded) {
		this.recorded = recorded;
	}

	static ClazzMetricsSnapshot of(Collection<Clazz> clazzes) {
		return new ClazzMetricsSnapshot(clazzes.stream().map(RecordedMetrics::of).toList());
	}

	void restore() {
		recorded.forEach(RecordedMetrics::writeBack);
	}

	// Boxed on purpose: a metric that was never computed is null and must stay null.
	private record RecordedMetrics(Clazz clazz, Double similarity, Double avgSimilarityWithRefClazzes) {

		static RecordedMetrics of(Clazz clazz) {
			return new RecordedMetrics(clazz, clazz.getSimilarity(), clazz.getAvgSimilarityWithRefClazzes());
		}

		void writeBack() {
			clazz.setSimilarity(similarity);
			clazz.setAvgSimilarityWithRefClazzes(avgSimilarityWithRefClazzes);
		}

	}

}
