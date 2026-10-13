package com.archanchor.dto.projects;

/**
 * A project created from a repository, with the exact commit it was taken from so that an
 * analysis can be traced back to the code it ran on.
 */
public record ImportedProjectDTO(String name, String repositoryUrl, String commitSha) {
}
