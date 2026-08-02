package com.taskmanager.web.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionReturnsUnifiedFormat() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleApiException(new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.getStatus());
        assertEquals("Access denied", body.getMessage());
        assertNotNull(body.getTimestamp());
        assertEquals(Instant.class, body.getTimestamp().getClass());
    }
}
