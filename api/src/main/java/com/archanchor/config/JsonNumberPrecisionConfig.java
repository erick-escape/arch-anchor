package com.archanchor.config;

import com.archanchor.util.FourDecimalPlaces;
import com.fasterxml.jackson.databind.Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link FourDecimalPlaces} on the {@code ObjectMapper} that Spring Boot builds
 * for the REST responses: Boot adds every {@link Module} bean to it (#71).
 */
@Configuration
public class JsonNumberPrecisionConfig {

	@Bean
	public Module fourDecimalPlacesModule() {
		return FourDecimalPlaces.jacksonModule();
	}

}
