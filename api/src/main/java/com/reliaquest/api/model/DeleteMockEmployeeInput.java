package com.reliaquest.api.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteMockEmployeeInput {

    private String name;
}
