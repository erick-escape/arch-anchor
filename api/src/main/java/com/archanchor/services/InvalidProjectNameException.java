package com.archanchor.services;

/**
 * A project name that cannot be used as a directory under the projects root.
 */
public class InvalidProjectNameException extends RuntimeException {

	public InvalidProjectNameException(String message) {
		super(message);
	}

}
