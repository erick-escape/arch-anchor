package com.archanchor.services.repositoryImport;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * {@link RepositoryCloner} backed by JGit, so the server needs no git binary. It lists
 * the remote's refs first, which tells an unreadable repository apart from a missing
 * branch and lets the clone fetch that one ref at depth 1 instead of the tip of every
 * branch.
 */
@Component
public class JGitRepositoryCloner implements RepositoryCloner {

	// GitHub accepts any non-empty user name next to a personal access token; this is the
	// one its documentation uses for token-based HTTPS access.
	private static final String TOKEN_USER_NAME = "x-access-token";

	private final int timeoutSeconds;

	public JGitRepositoryCloner(@Value("${archanchor.import.timeout-seconds:60}") int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	@Override
	public String cloneShallow(CloneRequest request, Path target) {
		String ref = refToClone(request, listRemoteRefs(request));
		try (Git git = cloneCommand(request, ref, target).call()) {
			return checkedOutCommit(git);
		}
		catch (GitAPIException | JGitInternalException | IOException e) {
			throw new RepositoryCloneFailedException("Could not clone " + request.uri() + ": " + e.getMessage(), e);
		}
	}

	private Map<String, Ref> listRemoteRefs(CloneRequest request) {
		try {
			return withTransportSettings(Git.lsRemoteRepository().setRemote(request.uri().toString()), request)
				.callAsMap();
		}
		catch (GitAPIException | JGitInternalException e) {
			throw translateListingFailure(request, e);
		}
	}

	/**
	 * Picks the single ref to fetch. Returns null only when the remote does not say which
	 * branch HEAD points to; the clone then falls back to JGit's default of all branches.
	 */
	private String refToClone(CloneRequest request, Map<String, Ref> remoteRefs) {
		if (remoteRefs.isEmpty()) {
			throw new RepositoryRefNotFoundException("Repository " + request.uri() + " has no commits to import");
		}
		if (!request.hasRef()) {
			Ref head = remoteRefs.get(Constants.HEAD);
			return head != null && head.isSymbolic() ? head.getTarget().getName() : null;
		}
		for (String candidate : List.of(Constants.R_HEADS + request.ref(), Constants.R_TAGS + request.ref(),
				request.ref())) {
			if (remoteRefs.containsKey(candidate)) {
				return candidate;
			}
		}
		throw new RepositoryRefNotFoundException(
				"Expected the name of a branch or tag in " + request.uri() + ", got '" + request.ref() + "'");
	}

	private CloneCommand cloneCommand(CloneRequest request, String ref, Path target) {
		CloneCommand command = Git.cloneRepository()
			.setURI(request.uri().toString())
			.setDirectory(target.toFile())
			.setDepth(1)
			.setNoTags()
			.setCloneSubmodules(false);
		if (ref != null) {
			command.setBranch(ref).setBranchesToClone(List.of(ref));
		}
		return withTransportSettings(command, request);
	}

	private <C extends TransportCommand<C, ?>> C withTransportSettings(C command, CloneRequest request) {
		command.setTimeout(timeoutSeconds);
		if (request.hasAccessToken()) {
			// Passed as credentials, never embedded in the URI, so it is not written to
			// .git/config or echoed in JGit's error messages.
			command.setCredentialsProvider(
					new UsernamePasswordCredentialsProvider(TOKEN_USER_NAME, request.accessToken()));
		}
		return command;
	}

	private String checkedOutCommit(Git git) throws IOException {
		ObjectId head = git.getRepository().resolve(Constants.HEAD);
		if (head == null) {
			throw new IOException("the clone has no HEAD commit");
		}
		return head.name();
	}

	private RuntimeException translateListingFailure(CloneRequest request, Exception failure) {
		if (isNetworkFailure(failure)) {
			return new RepositoryCloneFailedException("Could not reach " + request.uri() + ": " + failure.getMessage(),
					failure);
		}
		String reason = request.hasAccessToken() ? "or the access token cannot read it"
				: "or it is private; private repositories need an access token";
		return new RepositoryNotAccessibleException("Repository " + request.uri() + " was not found, " + reason,
				failure);
	}

	private static boolean isNetworkFailure(Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof SocketException || cause instanceof UnknownHostException
					|| cause instanceof InterruptedIOException || cause instanceof SSLException) {
				return true;
			}
		}
		return false;
	}

}
