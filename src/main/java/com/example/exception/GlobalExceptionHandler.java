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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse build(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.name())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }

    // ========================= 400 BAD REQUEST =========================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformed(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String message = "Malformed JSON request";
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof com.fasterxml.jackson.core.JsonParseException jpe) {
            message = "Invalid JSON syntax. Check for missing quotes, commas, or incorrect values.";

            // Optional: include location
            if (jpe.getLocation() != null) {
                message += " Error at line " + jpe.getLocation().getLineNr() +
                           ", column " + jpe.getLocation().getColumnNr();
            }
        }

        else if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {

            String fieldName = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("field");

            Class<?> targetType = ife.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                String allowedValues = Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));

                message = "Invalid value for '" + fieldName +
                        "'. Allowed values: " + allowedValues;
            } else {
                message = "Invalid value for '" + fieldName +
                        "'. Expected type: " + targetType.getSimpleName();
            }
        }

        else if (cause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException mie) {

            String fieldName = mie.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("field");

            message = "Invalid input for '" + fieldName + "'";
        }

        log.warn("Malformed JSON: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(build(HttpStatus.BAD_REQUEST, message, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, List<String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        fieldError -> fieldError.getField(),
                        Collectors.mapping(
                                fieldError -> fieldError.getDefaultMessage(),
                                Collectors.toList()
                        )
                ));

        log.warn("Validation failed: {}", errors);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message("Validation failed")
                .path(request.getRequestURI())
                .fieldErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(build(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    // ========================= 401 UNAUTHORIZED =========================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(HttpStatus.UNAUTHORIZED, "Authentication failed", request));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Invalid login attempt for request: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }

    // ========================= 403 FORBIDDEN =========================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this resource", request));
    }

    // ========================= 404 NOT FOUND =========================

    @ExceptionHandler({
        ResourceNotFoundException.class,
        UserNotFoundException.class,
        ProfileNotCreatedException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    // ========================= 409 CONFLICT =========================

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {

        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    // ========================= 422 UNPROCESSABLE ENTITY =========================

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request));
    }

    // ========================= 500 INTERNAL SERVER ERROR =========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled system error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unexpected system error occurred", request));
    }
}