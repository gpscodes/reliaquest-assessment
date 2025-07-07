package com.reliaquest.api.model;

import lombok.Data;

@Data
public class SingleEmployeeApiResponse {

    private String status;
    private Employee data;
}
