package com.archanchor.services.architecturalAnalyses;

import com.archanchor.domains.clazz.Clazz;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClazzMetricsSnapshotTest {

	@Test
	void restorePutsBackBothMetricsOfEveryRecordedClass() {
		Clazz controller = clazzWithMetrics("OrderController", 0.8, 1.0);
		Clazz report = clazzWithMetrics("SalesReport", 0.5, 0.25);
		ClazzMetricsSnapshot snapshot = ClazzMetricsSnapshot.of(List.of(controller, report));

		overwriteMetrics(controller);
		overwriteMetrics(report);
		snapshot.restore();

		assertThat(controller.getSimilarity()).isEqualTo(0.8);
		assertThat(controller.getAvgSimilarityWithRefClazzes()).isEqualTo(1.0);
		assertThat(report.getSimilarity()).isEqualTo(0.5);
		assertThat(report.getAvgSimilarityWithRefClazzes()).isEqualTo(0.25);
	}

	@Test
	void restoreKeepsAMetricThatWasNeverComputedAsNull() {
		Clazz notYetAnalysed = clazzWithMetrics("OrderController", null, null);
		ClazzMetricsSnapshot snapshot = ClazzMetricsSnapshot.of(List.of(notYetAnalysed));

		overwriteMetrics(notYetAnalysed);
		snapshot.restore();

		assertThat(notYetAnalysed.getSimilarity()).isNull();
		assertThat(notYetAnalysed.getAvgSimilarityWithRefClazzes()).isNull();
	}

	@Test
	void restoreLeavesClassesOutsideTheSnapshotAlone() {
		Clazz recorded = clazzWithMetrics("OrderController", 0.8, 1.0);
		Clazz outsider = clazzWithMetrics("SalesReport", 0.5, 0.25);
		ClazzMetricsSnapshot snapshot = ClazzMetricsSnapshot.of(List.of(recorded));

		overwriteMetrics(outsider);
		snapshot.restore();

		assertThat(outsider.getSimilarity()).isEqualTo(0.99);
	}

	private static Clazz clazzWithMetrics(String name, Double similarity, Double avgSimilarityWithRefClazzes) {
		return new Clazz(name, name, List.of(), similarity, avgSimilarityWithRefClazzes, "shop/web", "shop/web",
				"ALLOW");
	}

	private static void overwriteMetrics(Clazz clazz) {
		clazz.setSimilarity(0.99);
		clazz.setAvgSimilarityWithRefClazzes(0.99);
	}

}
