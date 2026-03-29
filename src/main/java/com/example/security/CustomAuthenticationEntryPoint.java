package com.example.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
	  private final ObjectMapper objectMapper;

	    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
	        this.objectMapper = objectMapper;
	    }


	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		
		ErrorResponse error = ErrorResponse.builder()
				.timestamp(Instant.now())
				.status(HttpStatus.UNAUTHORIZED.value())
				.error(HttpStatus.UNAUTHORIZED.name())
				.message("Authentication required or token is invalid")
				.path(request.getRequestURI())
				.build();
		
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json");
		
		objectMapper.writeValue(response.getOutputStream(), error);

	}

}
