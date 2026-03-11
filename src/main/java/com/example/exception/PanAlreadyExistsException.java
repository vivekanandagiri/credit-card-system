package com.example.exception;

public class PanAlreadyExistsException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PanAlreadyExistsException(String pan) {
        super("Customer already exists with PAN: " + pan);
    }
}