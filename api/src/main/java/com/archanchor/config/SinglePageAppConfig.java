package com.archanchor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the built frontend from {@code classpath:/static/}, where the Docker build puts
 * {@code web/dist}, so that the packaged tool runs as one process on one port (#77). It
 * takes the place of Spring Boot's own handler for {@code /**}, which Boot skips once a
 * handler for that pattern is registered, and adds {@link IndexHtmlFallback} for the
 * frontend's routes. Controllers are matched before resource handlers, so {@code /api/**}
 * is untouched.
 */
@Configuration
public class SinglePageAppConfig implements WebMvcConfigurer {

	private static final String BUILT_FRONTEND = "classpath:/static/";

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations(BUILT_FRONTEND)
			.resourceChain(true)
			.addResolver(new IndexHtmlFallback());
	}

}
