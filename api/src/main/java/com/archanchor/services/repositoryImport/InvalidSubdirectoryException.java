package com.archanchor.services.repositoryImport;

/**
 * The subdirectory the user named is not a relative path inside the repository.
 */
public class InvalidSubdirectoryException extends RuntimeException {

	public InvalidSubdirectoryException(String message) {
		super(message);
	}

}
