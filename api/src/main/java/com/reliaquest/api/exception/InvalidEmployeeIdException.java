package com.reliaquest.api.exception;

public class InvalidEmployeeIdException extends RuntimeException {
    public InvalidEmployeeIdException(String id) {
        super("Invalid employee ID: '" + id + "'");
    }
}
