package com.archanchor.services.repositoryImport;

import java.nio.file.Path;

/**
 * Fetches a snapshot of a remote repository into a local directory. The interface belongs
 * to this project so that callers and tests never depend on the git library behind it.
 */
public interface RepositoryCloner {

	/**
	 * Checks out the tip of the requested branch or tag, or of the remote's default
	 * branch, into {@code target}, without history beyond that commit.
	 *
	 * <pre>
	 * String sha = cloner.cloneShallow(new CloneRequest(uri, null, null), stagingDirectory);
	 * </pre>
	 * @return the full SHA of the checked-out commit
	 * @throws RepositoryNotAccessibleException when the repository does not exist or the
	 * request cannot read it
	 * @throws RepositoryRefNotFoundException when the branch or tag does not exist, or
	 * the repository has no commits
	 * @throws RepositoryCloneFailedException when the transfer itself fails
	 */
	String cloneShallow(CloneRequest request, Path target);

}
