package com.archanchor.util;

/**
 * How secrets appear in {@code toString()} output and therefore in logs.
 */
public final class SecretRedaction {

	private SecretRedaction() {
	}

	/**
	 * Describes a secret without revealing it.
	 *
	 * <pre>
	 * SecretRedaction.describe("github_pat_..."); // "&lt;redacted&gt;"
	 * SecretRedaction.describe(null); // "&lt;none&gt;"
	 * </pre>
	 */
	public static String describe(String secret) {
		return secret == null || secret.isBlank() ? "<none>" : "<redacted>";
	}

}
