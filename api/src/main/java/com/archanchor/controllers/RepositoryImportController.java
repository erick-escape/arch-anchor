package com.archanchor.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.archanchor.dto.projects.ImportedProjectDTO;
import com.archanchor.dto.projects.RepositoryImportRequest;
import com.archanchor.services.InvalidProjectNameException;
import com.archanchor.services.ProjectAlreadyExistsException;
import com.archanchor.services.repositoryImport.InvalidRepositoryUrlException;
import com.archanchor.services.repositoryImport.InvalidSubdirectoryException;
import com.archanchor.services.repositoryImport.MissingSourceDirectoryException;
import com.archanchor.services.repositoryImport.RepositoryCloneFailedException;
import com.archanchor.services.repositoryImport.RepositoryImportService;
import com.archanchor.services.repositoryImport.RepositoryNotAccessibleException;
import com.archanchor.services.repositoryImport.RepositoryRefNotFoundException;

import java.io.IOException;

/**
 * Creates projects from GitHub repositories. Failures come back as RFC 9457 problem
 * details whose {@code detail} is written for the user, because the import form shows it
 * as is.
 */
@Slf4j
@RestController
@RequestMapping("/api/projects")
public class RepositoryImportController {

	private final RepositoryImportService repositoryImportService;

	public RepositoryImportController(RepositoryImportService repositoryImportService) {
		this.repositoryImportService = repositoryImportService;
	}

	@PostMapping("/import")
	@ResponseStatus(HttpStatus.CREATED)
	public ImportedProjectDTO importRepository(@RequestBody RepositoryImportRequest request) throws IOException {
		return repositoryImportService.importRepository(request);
	}

	@ExceptionHandler({ InvalidRepositoryUrlException.class, InvalidProjectNameException.class,
			InvalidSubdirectoryException.class })
	ProblemDetail rejectInvalidInput(RuntimeException failure) {
		return problem(HttpStatus.BAD_REQUEST, failure);
	}

	@ExceptionHandler(RepositoryNotAccessibleException.class)
	ProblemDetail reportUnreadableRepository(RepositoryNotAccessibleException failure) {
		return problem(HttpStatus.NOT_FOUND, failure);
	}

	@ExceptionHandler(ProjectAlreadyExistsException.class)
	ProblemDetail reportTakenName(ProjectAlreadyExistsException failure) {
		return problem(HttpStatus.CONFLICT, failure);
	}

	@ExceptionHandler({ RepositoryRefNotFoundException.class, MissingSourceDirectoryException.class })
	ProblemDetail reportUnusableContent(RuntimeException failure) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, failure);
	}

	@ExceptionHandler(RepositoryCloneFailedException.class)
	ProblemDetail reportFailedTransfer(RepositoryCloneFailedException failure) {
		log.warn("Repository import failed: {}", failure.getMessage(), failure);
		return problem(HttpStatus.BAD_GATEWAY, failure);
	}

	private static ProblemDetail problem(HttpStatus status, RuntimeException failure) {
		return ProblemDetail.forStatusAndDetail(status, failure.getMessage());
	}

}
