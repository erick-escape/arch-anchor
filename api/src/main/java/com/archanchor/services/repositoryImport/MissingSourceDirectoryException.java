package com.archanchor.services.repositoryImport;

/**
 * The directory chosen for analysis has no {@code src/} directly under it, so module
 * discovery would find nothing.
 */
public class MissingSourceDirectoryException extends RuntimeException {

	public MissingSourceDirectoryException(String message) {
		super(message);
	}

}
