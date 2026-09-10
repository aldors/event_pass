package com.aldo.event_pass.exception.ApiError;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

@Getter
public class ApiError {
    
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;

    public ApiError(int status, String message, List<String> errors) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }
}
