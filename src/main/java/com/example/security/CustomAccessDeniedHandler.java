package com.example.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
	 private final ObjectMapper objectMapper;

	    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
	        this.objectMapper = objectMapper;
	    }

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpServletResponse.SC_FORBIDDEN,
				"FORBIDDEN",
				"You do not have permission to access this resource",
				request.getRequestURI()
				);
		
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json");
		objectMapper.writeValue(response.getOutputStream(), error);

	}

}
