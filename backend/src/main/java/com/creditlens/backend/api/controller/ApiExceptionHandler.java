package com.creditlens.backend.api.controller;

import com.creditlens.backend.api.dto.ApiProblemDto;
import com.creditlens.backend.application.ClientRequestConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ClientRequestConflictException.class)
    public ResponseEntity<ApiProblemDto> handleClientRequestConflict(
            ClientRequestConflictException exception,
            HttpServletRequest request
    ) {
        UUID correlationId = correlationId(request.getHeader("X-Correlation-Id"));
        ApiProblemDto problem = new ApiProblemDto(
                URI.create("https://credit-lens.local/problems/client-request-id-conflict"),
                "Client request ID conflict",
                409,
                exception.getMessage(),
                URI.create(request.getRequestURI()),
                correlationId
        );
        return ResponseEntity.status(409)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header("X-Correlation-Id", correlationId.toString())
                .body(problem);
    }

    private UUID correlationId(String header) {
        if (header != null) {
            try {
                return UUID.fromString(header);
            } catch (IllegalArgumentException ignored) {
                // An invalid correlation ID is replaced and never echoed into an error payload.
            }
        }
        return UUID.randomUUID();
    }
}
