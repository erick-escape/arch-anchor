// Mirrors ProjectDetailDTO / ProjectsListResponseDTO in api/.

export interface ProjectSummary {
  name: string;
}

export interface ProjectsListResponse {
  projects: ProjectSummary[];
}
