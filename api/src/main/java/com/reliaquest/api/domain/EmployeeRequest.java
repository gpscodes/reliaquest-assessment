package com.reliaquest.api.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Salary is required")
    @Min(value = 1, message = "Salary must be greater than 0")
    private Integer salary;

    @NotNull(message = "Age is required")
    @Min(value = 16, message = "Minimum age is 16")
    private Integer age;

    @NotBlank(message = "Title is required")
    private String title;
}
