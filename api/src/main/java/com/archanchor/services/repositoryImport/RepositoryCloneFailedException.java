package com.archanchor.services.repositoryImport;

/**
 * Fetching the repository failed for a reason outside the user's input: the network, a
 * timeout, or the remote breaking off the transfer.
 */
public class RepositoryCloneFailedException extends RuntimeException {

	public RepositoryCloneFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
