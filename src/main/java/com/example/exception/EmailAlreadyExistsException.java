package com.example.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EmailAlreadyExistsException(String email) {
        super("User already exists with email: " + email);
    }
}