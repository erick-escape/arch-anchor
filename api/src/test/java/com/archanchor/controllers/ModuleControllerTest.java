package com.archanchor.controllers;

import org.junit.jupiter.api.Test;

import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.PERSISTENCE;
import static com.archanchor.services.architecturalAnalyses.AnalysedShopFixture.WEB;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModuleControllerTest {

	private final ShopBackend backend = ShopBackend.analysed();

	// The page reloads this after applying a recommendation; re-analysing would discard
	// the change, because module operations are never written back to the sources (#19).
	@Test
	void listsTheSavedModules() throws Exception {
		backend.mockMvc.perform(get("/api/module"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name", contains(WEB, PERSISTENCE)))
			.andExpect(jsonPath("$[0].refClazzes[0].name").value("CatalogController"));
	}

}
