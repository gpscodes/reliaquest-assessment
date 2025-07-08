package com.reliaquest.api.model;

import lombok.Data;

@Data
public class EmployeeApiResponse<T> {

    private T data;
    private String status;
    private String error;
}
