package com.archanchor.dto.projects;

/**
 * A project of {@code GET /api/projects}.
 *
 * @param source where it was imported from; null for a project uploaded as a folder
 */
public record ProjectDetailDTO(String name, ImportSourceDTO source) {
}
