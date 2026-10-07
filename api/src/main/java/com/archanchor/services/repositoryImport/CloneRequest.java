package com.archanchor.services.repositoryImport;

import com.archanchor.util.SecretRedaction;

import java.net.URI;

/**
 * What to clone: a repository, optionally a branch or tag, and optionally an access token
 * for private repositories. Blank fields count as absent and the rest are stripped.
 * {@link #toString()} redacts the token so that logging a request cannot leak it.
 *
 * <pre>
 * new CloneRequest(URI.create("https://github.com/owner/repo.git"), "v2.1", null);
 * </pre>
 */
public record CloneRequest(URI uri, String ref, String accessToken) {

	public CloneRequest {
		ref = strippedOrNull(ref);
		accessToken = strippedOrNull(accessToken);
	}

	public boolean hasRef() {
		return ref != null;
	}

	public boolean hasAccessToken() {
		return accessToken != null;
	}

	@Override
	public String toString() {
		return "CloneRequest[uri=" + uri + ", ref=" + ref + ", accessToken=" + SecretRedaction.describe(accessToken)
				+ "]";
	}

	private static String strippedOrNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
