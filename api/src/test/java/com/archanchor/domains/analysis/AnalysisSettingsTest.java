package com.archanchor.domains.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisSettingsTest {

	// The defaults keep the analyses as they were before #36.
	@Test
	void theDefaultsFilterNothing() {
		AnalysisSettings defaults = AnalysisSettings.DEFAULT;

		assertThat(defaults.allowsSplitHalfOf(1)).isTrue();
		assertThat(defaults.allowsMoveLeaving(1)).isTrue();
		assertThat(defaults.limitsMoveTargets()).isFalse();
		assertThat(defaults.judgesClassWith(0)).isTrue();
		assertThat(defaults.shows(0.0001)).isTrue();
		assertThat(defaults.allowsMergeChangingSimilarityBy(-1.0)).isTrue();
		assertThat(defaults.allowsMoveChangingSimilarityBy(-1.0)).isTrue();
		assertThat(defaults.allowsSplitChangingSimilarityBy(-1.0)).isTrue();
	}

	// #41: 0 keeps out every merge that lowers similarity; the limit is inclusive.
	@Test
	void aMinimumSimilarityChangeKeepsOutMergesBelowIt() {
		AnalysisSettings settings = new AnalysisSettings(1, 1, null, 0, 0.0, 0.0, null, null);

		assertThat(settings.allowsMergeChangingSimilarityBy(0.0)).isTrue();
		assertThat(settings.allowsMergeChangingSimilarityBy(-0.01)).isFalse();
	}

	// #73: the same limits for moves and splits, each on its own.
	@Test
	void minimumSimilarityChangesKeepOutMovesAndSplitsBelowThem() {
		AnalysisSettings settings = new AnalysisSettings(1, 1, null, 0, 0.0, null, 0.0, -0.05);

		assertThat(settings.allowsMoveChangingSimilarityBy(0.0)).isTrue();
		assertThat(settings.allowsMoveChangingSimilarityBy(-0.01)).isFalse();
		assertThat(settings.allowsSplitChangingSimilarityBy(-0.05)).isTrue();
		assertThat(settings.allowsSplitChangingSimilarityBy(-0.06)).isFalse();
		assertThat(settings.allowsMergeChangingSimilarityBy(-1.0)).isTrue();
	}

	@Test
	void aMoveOrSplitMinimumOutsideMinusOneToOneIsRejectedNamingIt() {
		assertThatThrownBy(() -> new AnalysisSettings(1, 1, null, 0, 0.0, null, -1.5, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("minMoveSimilarityChange")
			.hasMessageContaining("[-1, 1]")
			.hasMessageContaining("got -1.5");
		assertThatThrownBy(() -> new AnalysisSettings(1, 1, null, 0, 0.0, null, null, Double.NaN))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("minSplitSimilarityChange");
	}

	@Test
	void aMinimumSimilarityChangeOutsideMinusOneToOneIsRejected() {
		assertThatThrownBy(() -> new AnalysisSettings(1, 1, null, 0, 0.0, 1.5, null, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("minMergeSimilarityChange")
			.hasMessageContaining("[-1, 1]")
			.hasMessageContaining("no limit");
	}

	@Test
	void eachMinimumIsInclusive() {
		AnalysisSettings settings = new AnalysisSettings(2, 3, 1, 5, 0.2, null, null, null);

		assertThat(settings.allowsSplitHalfOf(2)).isTrue();
		assertThat(settings.allowsSplitHalfOf(1)).isFalse();
		assertThat(settings.allowsMoveLeaving(3)).isTrue();
		assertThat(settings.allowsMoveLeaving(2)).isFalse();
		assertThat(settings.judgesClassWith(5)).isTrue();
		assertThat(settings.judgesClassWith(4)).isFalse();
		assertThat(settings.shows(0.2)).isTrue();
		assertThat(settings.shows(0.19)).isFalse();
	}

	@Test
	void aMaximumOfMoveTargetsLimitsThem() {
		AnalysisSettings settings = new AnalysisSettings(1, 1, 2, 0, 0.0, null, null, null);

		assertThat(settings.limitsMoveTargets()).isTrue();
		assertThat(settings.maxMoveTargetsPerClass()).isEqualTo(2);
	}

	@Test
	void aSplitHalfOfNoClassIsRejectedNamingTheSettingAndTheExpectedRange() {
		assertThatThrownBy(() -> new AnalysisSettings(0, 1, null, 0, 0.0, null, null, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("minSplitModuleClasses")
			.hasMessageContaining(">= 1")
			.hasMessageContaining("got 0");
	}

	@Test
	void aMoveMayNotLeaveAnEmptyModule() {
		assertThatThrownBy(() -> new AnalysisSettings(1, 0, null, 0, 0.0, null, null, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("minSourceClassesAfterMove");
	}

	@Test
	void aMaximumOfNoMoveTargetIsRejected() {
		assertThatThrownBy(() -> new AnalysisSettings(1, 1, 0, 0, 0.0, null, null, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("maxMoveTargetsPerClass")
			.hasMessageContaining("no limit");
	}

	@Test
	void aNegativeDependencyCountIsRejected() {
		assertThatThrownBy(() -> new AnalysisSettings(1, 1, null, -1, 0.0, null, null, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("minClassDependencies")
			.hasMessageContaining(">= 0");
	}

	// A rate combines changes in [-1, 1] with weights summing to 1, so it never exceeds
	// 1.
	@Test
	void aMinimumRateOutsideZeroToOneIsRejected() {
		assertThatThrownBy(() -> new AnalysisSettings(1, 1, null, 0, 1.5, null, null, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class)
			.hasMessageContaining("minRecommendationRate")
			.hasMessageContaining("[0, 1]")
			.hasMessageContaining("1.5");
		assertThatThrownBy(() -> new AnalysisSettings(1, 1, null, 0, Double.NaN, null, null, null))
			.isInstanceOf(InvalidAnalysisSettingsException.class);
	}

}
