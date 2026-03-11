package com.example.exception;

import com.example.dto.response.ErrorResponse;   

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse build(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                message,
                request.getRequestURI()
        );
    }

    // ========================= 400 BAD REQUEST =========================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformed(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
    	
    	String message = "Malformed JSON request";

        if (ex.getMostSpecificCause() != null) {
            message = ex.getMostSpecificCause().getMessage();
        }


        log.warn("Malformed JSON: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(build(
                        HttpStatus.BAD_REQUEST,
                        message,
                        request
                ));
    }

    // Validation errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
          .getFieldErrors()
          .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        log.warn("Validation failed: {}", errors);

        ErrorResponse response = build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request
        );

        response.setFieldErrors(errors);
        
        return ResponseEntity.badRequest()
                .body(response);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessRuleException ex,
            HttpServletRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        request
                ));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(build(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        request
                ));
    }
    
  
    // ========================= 401 UNAUTHORIZED =========================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication failed",
                        request
                ));
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Invalid login attempt");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(
                        HttpStatus.UNAUTHORIZED,
                        ex.getMessage(),
                        request
                ));
    }
    
 // ========================= 403 FORBIDDEN =========================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
    	
    	log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(
                        HttpStatus.FORBIDDEN,
                        "You do not have permission to access this resource",
                        request
                ));
    }


    // ========================= 404 NOT FOUND =========================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
    	
    	log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage(),
                        request
                ));
    }

  // ========================= 409 CONFLICT =========================

    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {

        log.warn("Conflict: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(
                        HttpStatus.CONFLICT,
                        ex.getMessage(),
                        request
                ));
    }
    

 // ========================= 500 INTERNAL SERVER ERROR =========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(
            Exception ex,
            HttpServletRequest request) {

    	log.error("Unhandled system error at {} : {}", 
    	        request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unexpected system error occurred",
                        request
                ));
    }
    
    //Illegal Argument
    //BAD REQUEST EXCEPTION 400
  
}