package com.archanchor.services.repositoryImport;

import java.net.URI;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A GitHub repository named by a URL the user pasted, normalized so that every accepted
 * form clones over HTTPS from github.com. The host is fixed here instead of being taken
 * from the input, which keeps the importer, and any access token sent with the clone,
 * from being pointed at another server.
 *
 * <pre>
 * GitHubRepositoryUrl.parse("git@github.com:spring-projects/spring-petclinic.git").cloneUri();
 * // https://github.com/spring-projects/spring-petclinic.git
 * </pre>
 */
public record GitHubRepositoryUrl(String owner, String repository) {

	private static final String EXPECTED_SHAPE = "https://github.com/owner/repo";

	// GitHub's own limits: owners are alphanumeric or '-', up to 39 characters;
	// repositories may also contain '.' and '_', up to 100. The lazy repository group
	// leaves ".git" to the suffix.
	private static final String OWNER_AND_REPOSITORY = "(?<owner>[A-Za-z0-9-]{1,39})/(?<repository>[A-Za-z0-9._-]{1,100}?)(?:\\.git)?";

	private static final Pattern HTTPS_FORM = Pattern
		.compile("^(?:https://)?(?:www\\.)?github\\.com/" + OWNER_AND_REPOSITORY + "/?$");

	private static final Pattern SSH_FORM = Pattern.compile("^git@github\\.com:" + OWNER_AND_REPOSITORY + "$");

	private static final Pattern DEEP_LINK_FORM = Pattern
		.compile("^(?:https://)?(?:www\\.)?github\\.com/[^/]+/[^/]+/.+$");

	private static final Set<String> RESERVED_REPOSITORY_NAMES = Set.of(".", "..");

	/**
	 * Parses a repository URL in any form GitHub's UI offers for copying.
	 * @throws InvalidRepositoryUrlException when the URL names anything other than a
	 * repository on github.com
	 */
	public static GitHubRepositoryUrl parse(String rawUrl) {
		String candidate = withoutQueryOrFragment(rawUrl == null ? "" : rawUrl.trim());
		Matcher matcher = matchingForm(candidate);
		if (matcher == null || RESERVED_REPOSITORY_NAMES.contains(matcher.group("repository"))) {
			throw rejection(rawUrl, candidate);
		}
		return new GitHubRepositoryUrl(matcher.group("owner"), matcher.group("repository"));
	}

	public URI cloneUri() {
		return URI.create(browseUrl() + ".git");
	}

	public String browseUrl() {
		return "https://github.com/" + owner + "/" + repository;
	}

	private static String withoutQueryOrFragment(String url) {
		int end = url.length();
		for (char separator : new char[] { '?', '#' }) {
			int index = url.indexOf(separator);
			if (index >= 0 && index < end) {
				end = index;
			}
		}
		return url.substring(0, end);
	}

	private static Matcher matchingForm(String candidate) {
		for (Pattern form : new Pattern[] { HTTPS_FORM, SSH_FORM }) {
			Matcher matcher = form.matcher(candidate);
			if (matcher.matches()) {
				return matcher;
			}
		}
		return null;
	}

	private static InvalidRepositoryUrlException rejection(String rawUrl, String candidate) {
		if (DEEP_LINK_FORM.matcher(candidate).matches()) {
			return new InvalidRepositoryUrlException("Expected a repository URL like " + EXPECTED_SHAPE + ", got '"
					+ rawUrl + "'. To import a branch or a folder, paste the repository URL and fill in the branch "
					+ "and subdirectory fields.");
		}
		return new InvalidRepositoryUrlException(
				"Expected a GitHub repository URL like " + EXPECTED_SHAPE + ", got '" + rawUrl + "'");
	}

}
