package com.archanchor.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** #71: the converter that writes every REST response rounds to four decimal places. */
@SpringBootTest
class JsonNumberPrecisionConfigTest {

	@Autowired
	private MappingJackson2HttpMessageConverter responseConverter;

	@Test
	void restResponsesCarryAtMostFourDecimalPlaces() throws Exception {
		String written = responseConverter.getObjectMapper().writeValueAsString(List.of(0.4444444444444444));

		assertThat(written).isEqualTo("[0.4444]");
	}

}
