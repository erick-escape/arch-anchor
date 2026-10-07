package com.archanchor.services.repositoryImport;

/**
 * The URL the user pasted does not name a GitHub repository the importer can clone.
 */
public class InvalidRepositoryUrlException extends RuntimeException {

	public InvalidRepositoryUrlException(String message) {
		super(message);
	}

}
