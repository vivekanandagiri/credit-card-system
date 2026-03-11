package com.example.exception;

public class AccountLockedException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AccountLockedException() {
        super("Account is locked. Please contact support.");
    }
}