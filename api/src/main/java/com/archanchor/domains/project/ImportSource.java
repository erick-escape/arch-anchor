package com.archanchor.domains.project;

/**
 * Where an imported project came from (#66): the repository, the subdirectory and ref it
 * was imported at, if any, and the commit that was cloned. An uploaded folder has none.
 *
 * <pre>
 * new ImportSource("https://github.com/erick-escape/arch-anchor", "examples/bookstore", null, "3ab2e30…");
 * </pre>
 *
 * @param subdirectory null for the repository root
 * @param ref null for the repository's default branch
 */
public record ImportSource(String repositoryUrl, String subdirectory, String ref, String commitSha) {
}
