package com.archanchor.services;

import com.archanchor.domains.module.Module;
import com.archanchor.dto.module.ModuleDTO;
import com.archanchor.mapstruct.ModuleMapperImpl;
import com.archanchor.services.architecturalAnalyses.AnalysedShopFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A module remembers whether the architect chose its reference classes, so that changing
 * the election weights re-elects every module but those (#27).
 */
class HandPickedReferenceClassesTest {

	private static final String WEB_ID = AnalysedShopFixture.idOf(AnalysedShopFixture.WEB);

	private static final String ORDER_CONTROLLER_ID = AnalysedShopFixture.idOf("OrderController");

	private final InMemoryModuleService moduleService = new InMemoryModuleService();

	@BeforeEach
	void saveTheAnalysedShop() {
		List<Module> analysed = AnalysedShopFixture.webAndPersistence(moduleService);
		moduleService.saveModules(analysed.stream().map(new ModuleMapperImpl()::toDto).toList());
	}

	@Test
	void electedReferenceClassesAreNotMarkedAsChosenByHand() {
		assertThat(moduleService.getModulesFromFile()).extracting(ModuleDTO::refClazzesChosenByHand)
			.containsExactly(false, false);
	}

	@Test
	void choosingReferenceClassesMarksThatModuleOnly() {
		ModuleDTO web = moduleService.setRefClazzes(WEB_ID, List.of(ORDER_CONTROLLER_ID));

		assertThat(web.refClazzesChosenByHand()).isTrue();
		assertThat(moduleService.getModulesFromFile()).extracting(ModuleDTO::name, ModuleDTO::refClazzesChosenByHand)
			.containsExactly(org.assertj.core.groups.Tuple.tuple(AnalysedShopFixture.WEB, true),
					org.assertj.core.groups.Tuple.tuple(AnalysedShopFixture.PERSISTENCE, false));
	}

	@Test
	void theMarkReachesTheModulesTheAnalysesRunOn() {
		moduleService.setRefClazzes(WEB_ID, List.of(ORDER_CONTROLLER_ID));

		assertThat(moduleService.savedModulesForAnalysis()).extracting(Module::isRefClazzesChosenByHand)
			.containsExactly(true, false);
	}

	@Test
	void anElectionClearsTheMark() {
		moduleService.setRefClazzes(WEB_ID, List.of(ORDER_CONTROLLER_ID));
		Module web = moduleService.savedModulesForAnalysis().get(0);

		moduleService.calculateClassSimilaritiesAndSelectRefClasses(web);

		assertThat(web.isRefClazzesChosenByHand()).isFalse();
	}

}
