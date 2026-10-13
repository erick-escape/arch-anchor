package com.archanchor.services;

import com.archanchor.domains.module.Module;
import com.archanchor.mapstruct.ModuleMapperImpl;
import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import com.archanchor.services.architecturalAnalyses.SplitModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModuleService#savedModulesForAnalysis}, which turns the saved modules
 * back into something the analyses can run on (#19).
 */
class SavedModulesForAnalysisTest {

	private final InMemoryModuleService moduleService = new InMemoryModuleService();

	@BeforeEach
	void saveTheAnalysedShop() {
		List<Module> analysed = AnalysedShopFixture.webAndPersistence(moduleService);
		moduleService.saveModules(analysed.stream().map(new ModuleMapperImpl()::toDto).toList());
	}

	@Test
	void everyReferenceClassIsTheSameObjectAsItsEntryInTheClassList() {
		List<Module> savedModules = moduleService.savedModulesForAnalysis();

		// Clazz has no equals(), so containsAll compares by identity, as the analyses do.
		assertThat(savedModules).hasSize(2)
			.allSatisfy(module -> assertThat(module.getClazzes()).containsAll(module.getRefClazzes()));
	}

	@Test
	void violationsAreRecomputedBecauseTheSavedModuleDoesNotCarryThem() {
		List<Module> savedModules = moduleService.savedModulesForAnalysis();

		assertThat(savedModules).extracting(Module::getName, Module::getViolations)
			.containsExactly(org.assertj.core.groups.Tuple.tuple(AnalysedShopFixture.WEB, 1),
					org.assertj.core.groups.Tuple.tuple(AnalysedShopFixture.PERSISTENCE, 0));
	}

	@Test
	void aSplitOfASavedModuleListsEachClassOnce() {
		SplitModule splitModule = new SplitModule(moduleService);

		splitModule.execute(moduleService.savedModulesForAnalysis());

		// With a reference class that is a second object, SplitModule would put it in the
		// first half twice: once as the reference class and once as an ordinary class.
		assertThat(splitModule.getResults()).singleElement().satisfies(split -> {
			List<String> classIds = new ArrayList<>(split.module1ClassIds);
			classIds.addAll(split.module2ClassIds);
			assertThat(classIds).doesNotHaveDuplicates().hasSize(4);
		});
	}

}
