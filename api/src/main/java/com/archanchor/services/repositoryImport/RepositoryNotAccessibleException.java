package com.archanchor.services.repositoryImport;

/**
 * The remote answered but would not serve the repository: it does not exist, or it is
 * private and the request carried no token able to read it. GitHub deliberately does not
 * tell these cases apart.
 */
public class RepositoryNotAccessibleException extends RuntimeException {

	public RepositoryNotAccessibleException(String message, Throwable cause) {
		super(message, cause);
	}

}
