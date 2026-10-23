package com.archanchor.config;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Finds a file of the built frontend, or its {@code index.html} when there is no such
 * file: the frontend's routes ({@code /projects}, {@code /analyze/:projectName}) exist
 * only in the browser, so a reload of one must get the app, which then routes itself
 * (#77). Never for a path under {@code api/}, so that a mistyped API call stays a 404.
 * Without a built frontend, as under the dev server, there is no index and nothing
 * changes.
 *
 * <pre>
 * registry.addResourceHandler("/**").addResourceLocations("classpath:/static/")
 *     .resourceChain(true).addResolver(new IndexHtmlFallback());
 * </pre>
 */
class IndexHtmlFallback extends PathResourceResolver {

	private static final String INDEX = "index.html";

	private static final String API_PREFIX = "api/";

	@Override
	protected Resource getResource(String resourcePath, Resource location) throws IOException {
		Resource requested = super.getResource(resourcePath, location);
		if (requested != null || resourcePath.startsWith(API_PREFIX)) {
			return requested;
		}
		return super.getResource(INDEX, location);
	}

}
