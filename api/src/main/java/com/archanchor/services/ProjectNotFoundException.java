package com.archanchor.services;

/**
 * A request named a project that is not under the projects root.
 */
public class ProjectNotFoundException extends RuntimeException {

	public ProjectNotFoundException(String projectName) {
		super("No project named '" + projectName + "'; expected one of those GET /api/projects lists");
	}

}
