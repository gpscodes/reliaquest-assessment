package com.reliaquest.api.domain;

import lombok.Data;

@Data
public class Employee {

    private String id;
    private String employee_name;
    private Integer employee_salary;
    private Integer employee_age;
    private String employee_title;
    private String employee_email;
}
