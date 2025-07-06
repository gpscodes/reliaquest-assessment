package com.reliaquest.api.service;

import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.EmployeeRequest;
import com.reliaquest.api.domain.EmployeeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.reliaquest.api.utils.EmployeeConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final RestTemplate restTemplate;

    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees from mock API.");
        EmployeeResponse response = restTemplate.getForObject(MOCK_API_URL, EmployeeResponse.class);
        if (response != null && response.getData() != null) {
            log.info("Retrieved {} employees.", response.getData().size());
            return response.getData();
        } else {
            log.warn("No employees found or response was null.");
            return List.of();
        }
    }

    public Employee getEmployeeById(String id) {
        String url = MOCK_API_URL.concat(FORWARD_SLASH).concat(id);
        log.info("Fetching employee by ID: {}", id);
        try {
            EmployeeResponse response = restTemplate.getForObject(url, EmployeeResponse.class);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().get(FIRST_INDEX);
            }
            throw new NoSuchElementException("Employee not found for ID: " + id);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Employee not found: {}", id);
            throw new NoSuchElementException("Employee not found for ID: " + id);
        }
    }

    public List<Employee> getEmployeesByNameSearch(String name) {
        log.info("Searching employees with name containing: {}", name);
        List<Employee> allEmployees = getAllEmployees();
        return allEmployees.stream()
                .filter(emp -> emp.getEmployee_name() != null &&
                        emp.getEmployee_name().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }


    public Integer getHighestSalaryOfEmployees() {
        log.info("Fetching highest employee salary...");
        List<Employee> employees = getAllEmployees();

        return employees.stream()
                .map(Employee::getEmployee_salary)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    public Employee createEmployee(EmployeeRequest request) {
        log.info("Creating new employee: {}", request.getName());
        String url = MOCK_API_URL;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmployeeRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Employee> response =
                restTemplate.postForEntity(url, entity, Employee.class);

        if (response.getBody() != null && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to create employee.");
        }
    }

    public List<String> getTop10HighestEarningEmployeeNames() {
        log.info("Fetching top 10 highest earning employee names...");
        List<Employee> employees = getAllEmployees();

        return employees.stream()
                .sorted(Comparator.comparing(Employee::getEmployee_salary).reversed())
                .limit(10)
                .map(Employee::getEmployee_name)
                .collect(Collectors.toList());
    }

    public String deleteEmployeeById(String id) {
        log.info("Attempting to delete employee with ID: {}", id);

        // Step 1: Look up the employee by ID
        Employee employee = getEmployeeById(id);
        if (employee == null || employee.getEmployee_name() == null) {
            throw new NoSuchElementException("Employee not found with ID: " + id);
        }

        String name = employee.getEmployee_name();
        String url = MOCK_API_URL + "/" + name;

        // Step 2: Perform DELETE with error handling
        try {
            restTemplate.delete(url);
            log.info("Deleted employee: {}", name);
            return name;
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Employee not found for deletion: {}", name);
            throw new NoSuchElementException("Unable to delete. Employee with name '" + name + "' not found.");
        } catch (HttpClientErrorException ex) {
            log.error("Error occurred while deleting employee: {}", ex.getMessage());
            throw new RuntimeException("Failed to delete employee: " + ex.getStatusCode());
        }
    }
}
