package com.reliaquest.api.domain;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeResponse {

    private List<Employee> data;
    private String status;
}
