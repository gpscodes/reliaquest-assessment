package com.reliaquest.api.service.impl;

import static com.reliaquest.api.utils.EmployeeConstants.*;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeRequest;
import com.reliaquest.api.model.EmployeeApiResponse;
import com.reliaquest.api.model.SingleEmployeeApiResponse;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.InvalidEmployeeIdException;
import java.util.*;
import java.util.stream.Collectors;

import com.reliaquest.api.service.IEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService implements IEmployeeService {

    private final RestTemplate restTemplate;

    @Override
    @Cacheable("employees")
    public List<Employee> getAllEmployees() {
        log.info("EmployeeService : Fetching all employees from mock API.");
        EmployeeApiResponse response = restTemplate.getForObject(MOCK_API_URL, EmployeeApiResponse.class);
        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            log.info("EmployeeService : Retrieved {} employees.", response.getData().size());
            return response.getData();
        } else {
            log.warn("EmployeeService : No employees found or response was null.");
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = "employeeById", key = "#id")
    public Employee getEmployeeById(String id) {
        validateId(id);

        String url = MOCK_API_URL + FORWARD_SLASH + id;
        log.info("EmployeeService : Fetching employee by ID: {}", id);

        try {
            ResponseEntity<SingleEmployeeApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            if (Objects.requireNonNull(response.getBody()).getData() != null) {
                return response.getBody().getData();
            }
            log.info("EmployeeService : No employee data found for ID: {}", id);
            throw new EmployeeNotFoundException(id);
        } catch (HttpClientErrorException.NotFound exception) {
            log.info("EmployeeService : Employee not found for ID: {}", id);
            throw new EmployeeNotFoundException(id);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("EmployeeService : Rate limit hit when calling mock API: {}", e.getMessage());
            throw new EmployeeServiceException("Rate limit exceeded. Please try again later.", e);
        } catch (Exception exception) {
            log.error("EmployeeService : Error occurred while fetching employee ID {}: {}", id, exception.getMessage(), exception);
            throw new EmployeeServiceException("Failed to fetch employee with ID: " + id, exception);
        }
    }

    @Override
    public List<Employee> getEmployeesByNameSearch(String name) {
        if (name == null || name.isBlank()) {
            log.warn("EmployeeService : Empty or null name provided for search");
            return Collections.emptyList();
        }

        log.info("EmployeeService : Searching employees with name containing: '{}'", name);

        String lowerCaseSearch = name.trim().toLowerCase();

        return getEmployees().stream()
                .filter(emp -> {
                    String empName = emp.getEmployee_name();
                    return empName != null && empName.toLowerCase().contains(lowerCaseSearch);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Integer getHighestSalaryOfEmployees() {
        log.info("EmployeeService : Fetching highest employee salary...");

        return getEmployees().stream()
                .map(Employee::getEmployee_salary)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0);
    }

    @Override
    @CacheEvict(value = "employees", allEntries = true)
    public Employee createEmployee(EmployeeRequest request) {
        log.info("EmployeeService : Creating new employee: {}", request.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmployeeRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Employee> response = restTemplate.postForEntity(MOCK_API_URL, entity, Employee.class);

            if (response.getBody() != null) {
                log.info("EmployeeService : Employee created successfully: {}", response.getBody());
                return response.getBody();
            } else {
                log.error("EmployeeService : Empty response while creating employee");
                throw new EmployeeServiceException("EmployeeService : Failed to create employee: empty response body");
            }

        } catch (RestClientException ex) {
            log.error("EmployeeService : Error while creating employee: {}", ex.getMessage(), ex);
            throw new EmployeeServiceException("Failed to create employee due to API error", ex);
        }
    }

    @Override
    public List<String> getTop10HighestEarningEmployeeNames() {
        log.info("EmployeeService : Fetching top 10 highest earning employee names...");

        return getEmployees().stream()
                .sorted(Comparator.comparing(Employee::getEmployee_salary).reversed())
                .limit(10)
                .map(Employee::getEmployee_name)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "employeeById", key = "#id")
    public void deleteEmployeeById(String id) {
        log.info("EmployeeService : Attempting to delete employee with ID: {}", id);
        Employee employee = getEmployeeById(id);
        final String name = employee.getEmployee_name();

        if (name == null || name.isBlank()) {
            log.error("EmployeeService : Employee name is missing for ID: {}", id);
            throw new EmployeeServiceException("Cannot delete employee: name is missing");
        }

        String url = MOCK_API_URL + FORWARD_SLASH + name;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("name", name);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    Map.class
            );

            log.info("EmployeeService : Delete response: {}", response.getBody());
            Boolean deleted = Optional.ofNullable((Boolean) response.getBody().get("data")).orElse(false);
            if (!deleted) {
                throw new EmployeeServiceException("EmployeeService : Employee not deleted as expected.");
            }
            log.info("EmployeeService : Successfully deleted employee: {}", name);
        } catch (HttpClientErrorException ex) {
            log.error("EmployeeService : HTTP error deleting employee id '{}': {}", id, ex.getMessage());
            throw new EmployeeServiceException("Failed to delete employee. HTTP Status: " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            log.error("EmployeeService : Unexpected error deleting employee id'{}': {}", id, ex.getMessage());
            throw new EmployeeServiceException("EmployeeService : Unexpected error occurred while deleting employee.", ex);
        }
    }

    private List<Employee> getEmployees() {
        List<Employee> employees = getAllEmployees();
        if (employees.isEmpty()) {
            log.warn("EmployeeService : No employees found to determine highest salary.");
            throw new EmployeeServiceException("EmployeeService : No employees found to determine highest salary.");
        }
        return employees;
    }

    private static void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidEmployeeIdException(id);
        }
    }
}
