package com.archanchor.services.repositoryImport;

/**
 * No parser finds sources in the directory chosen for analysis, so there would be nothing
 * to analyse (#87). The message says what each language expects and suggests the
 * directories that would work.
 */
public class MissingSourceDirectoryException extends RuntimeException {

	public MissingSourceDirectoryException(String message) {
		super(message);
	}

}
