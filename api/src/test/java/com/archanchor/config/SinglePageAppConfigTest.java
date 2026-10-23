package com.archanchor.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #77: the packaged tool serves the built frontend next to the API, on one port. The test
 * classpath holds a stand-in for the build in {@code static/}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SinglePageAppConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void servesTheAppAtTheRoot() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
	}

	@Test
	void servesTheAppOnAReloadOfAClientSideRoute() throws Exception {
		mockMvc.perform(get("/analyze/bookstore"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("ArchAnchor test page")));
	}

	@Test
	void servesTheBuiltAssets() throws Exception {
		mockMvc.perform(get("/assets/app.js"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("test asset")));
	}

	@Test
	void keepsAnUnknownApiPathANotFound() throws Exception {
		mockMvc.perform(get("/api/no-such-endpoint")).andExpect(status().isNotFound());
	}

}
