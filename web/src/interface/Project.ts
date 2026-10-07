// Mirrors ProjectDetailDTO / ProjectsListResponseDTO in api/.

export interface ProjectSummary {
  name: string;
}

export interface ProjectsListResponse {
  projects: ProjectSummary[];
}

// Mirrors RepositoryImportRequest / ImportedProjectDTO in api/. Only repositoryUrl is required.
export interface RepositoryImportRequest {
  repositoryUrl: string;
  ref?: string;
  subdirectory?: string;
  projectName?: string;
  accessToken?: string;
}

export interface ImportedProject {
  name: string;
  repositoryUrl: string;
  commitSha: string;
}

// RFC 9457 body the API returns for failed imports; `detail` is written for the user.
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
}
