package com.reliaquest.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EmployeeServiceException extends RuntimeException {
    public EmployeeServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmployeeServiceException(String message) {
        super(message);
    }
}
