package com.archanchor.services;

/**
 * A new project would overwrite one that is already in the projects root.
 */
public class ProjectAlreadyExistsException extends RuntimeException {

	public ProjectAlreadyExistsException(String projectName) {
		super("A project named '" + projectName + "' already exists; delete it first or choose another project name");
	}

}
