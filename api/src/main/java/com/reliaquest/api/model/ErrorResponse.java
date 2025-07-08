package com.reliaquest.api.model;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;
    private String error;
    private String timestamp;

    public ErrorResponse(String message, String error) {
        this.message = message;
        this.error = error;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
}
