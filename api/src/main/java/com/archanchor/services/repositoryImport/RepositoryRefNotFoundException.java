package com.archanchor.services.repositoryImport;

/**
 * The repository is readable but has no branch or tag to check out under the requested
 * name, or no commits at all.
 */
public class RepositoryRefNotFoundException extends RuntimeException {

	public RepositoryRefNotFoundException(String message) {
		super(message);
	}

}
