package com.example.exception;

public class ProfileNotCreatedException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ProfileNotCreatedException(String message) {
		super(message);
	}

}
