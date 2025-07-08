package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.controller.impl.EmployeeController;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeRequest;
import com.reliaquest.api.service.impl.EmployeeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee employee;
    private EmployeeRequest employeeRequest;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId("1");
        employee.setEmployee_name("Guru Prasad");
        employee.setEmployee_salary(100000);
        employee.setEmployee_age(30);
    }

    @Test
    void getAllEmployees_returnsList() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employee_name").value("Guru Prasad"));
    }

    @Test
    void getEmployeesByNameSearch_returnsFilteredResults() throws Exception {
        when(employeeService.getEmployeesByNameSearch("john")).thenReturn(List.of(employee));

        mockMvc.perform(get("/api/employees/search/john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employee_name").value("Guru Prasad"));
    }

    @Test
    void getEmployeeById_returnsEmployee() throws Exception {
        when(employeeService.getEmployeeById("1")).thenReturn(employee);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_name").value("Guru Prasad"));
    }

    @Test
    void getHighestSalaryOfEmployees_returnsInteger() throws Exception {
        when(employeeService.getHighestSalaryOfEmployees()).thenReturn(120000);

        mockMvc.perform(get("/api/employees/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().string("120000"));
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_returnsList() throws Exception {
        when(employeeService.getTop10HighestEarningEmployeeNames()).thenReturn(List.of("Guru Prasad", "Jane Smith"));

        mockMvc.perform(get("/api/employees/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void createEmployee_returnsCreatedEmployee() throws Exception {
        employeeRequest = new EmployeeRequest();
        employeeRequest.setName("Guru Prasad");
        employeeRequest.setSalary(100000);
        employeeRequest.setAge(30);
        employeeRequest.setTitle("Software Engineer");
        when(employeeService.createEmployee(any())).thenReturn(employee);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee_name").value("Guru Prasad"));
    }

    @Test
    void deleteEmployeeById_returnsNoContent() throws Exception {
        doNothing().when(employeeService).deleteEmployeeById("1");

        mockMvc.perform(delete("/api/employees/1")).andExpect(status().isNoContent());
    }
}
