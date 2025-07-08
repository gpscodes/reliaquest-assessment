package com.reliaquest.api.service.impl;

import static com.reliaquest.api.utils.EmployeeConstants.*;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.InvalidEmployeeIdException;
import com.reliaquest.api.model.*;
import com.reliaquest.api.service.IEmployeeService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.*;
import java.util.stream.Collectors;
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
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService implements IEmployeeService {

    private final RestTemplate restTemplate;

    private final WebClient webClient;

    @Override
    @Cacheable("employees")
    @Retry(name = "employeeService", fallbackMethod = "fallbackGetEmployee")
    @CircuitBreaker(name = "employeeService", fallbackMethod = "fallbackGetEmployee")
    public List<Employee> getAllEmployees() {
        log.info("EmployeeService : Fetching all employees from mock API.");
        ResponseEntity<EmployeeApiResponse<List<Employee>>> response = restTemplate.exchange(
                MOCK_API_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<EmployeeApiResponse<List<Employee>>>() {});
        if (response.getBody() != null && !response.getBody().getData().isEmpty()) {
            log.info(
                    "EmployeeService : Retrieved {} employees.",
                    response.getBody().getData().size());
            return response.getBody().getData();
        } else {
            log.warn("EmployeeService : No employees found or response was null.");
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = "employeeById", key = "#id")
    @Retry(name = "employeeService", fallbackMethod = "fallbackGetEmployee")
    @CircuitBreaker(name = "employeeService", fallbackMethod = "fallbackGetEmployee")
    public Employee getEmployeeById(String id) {
        validateId(id);

        String url = MOCK_API_URL + FORWARD_SLASH + id;
        log.info("EmployeeService : Fetching employee by ID: {}", id);

        try {
            ResponseEntity<EmployeeApiResponse<Employee>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<EmployeeApiResponse<Employee>>() {});
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
            log.error(
                    "EmployeeService : Error occurred while fetching employee ID {}: {}",
                    id,
                    exception.getMessage(),
                    exception);
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
    @Retry(name = "employeeService", fallbackMethod = "fallbackCreateEmployee")
    @CircuitBreaker(name = "employeeService", fallbackMethod = "fallbackCreateEmployee")
    public Employee createEmployee(EmployeeRequest request) {
        log.info("EmployeeService : Creating new employee: {}", request.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmployeeRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<EmployeeApiResponse<Employee>> response = restTemplate.exchange(
                    MOCK_API_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<EmployeeApiResponse<Employee>>() {});

            if (response.getBody() != null) {
                log.info("EmployeeService : Employee created successfully: {}", response.getBody());
                return response.getBody().getData();
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
    @CacheEvict(
            value = {"employeeById", "employees"},
            key = "#id",
            allEntries = true)
    @Retry(name = "employeeService", fallbackMethod = "fallbackDeleteEmployee")
    @CircuitBreaker(name = "employeeService", fallbackMethod = "fallbackDeleteEmployee")
    public void deleteEmployeeById(String id) {
        log.info("EmployeeService : Attempting to delete employee with ID: {}", id);
        Employee employee = getEmployeeById(id);
        final String name = employee.getEmployee_name();
        log.info("EmployeeService : Attempting to delete employee with ID: {} and Name: {}", id, name);

        if (name == null || name.isBlank()) {
            log.error("EmployeeService : Employee name is missing for ID: {}", id);
            throw new EmployeeServiceException("Cannot delete employee: name is missing");
        }

        String url = MOCK_API_URL + FORWARD_SLASH + name;

        DeleteMockEmployeeInput input =
                DeleteMockEmployeeInput.builder().name(name).build();

        try {
            EmployeeApiResponse<Boolean> response = webClient
                    .method(HttpMethod.DELETE)
                    .uri(MOCK_API_URL)
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<EmployeeApiResponse<Boolean>>() {})
                    .block();

            log.info("EmployeeService : Delete response: {}", response.getData());
            if (!response.getData()) {
                throw new EmployeeServiceException("EmployeeService : Employee not deleted as expected.");
            }
            log.info("EmployeeService : Successfully deleted employee: {}", name);
        } catch (HttpClientErrorException ex) {
            log.error("EmployeeService : HTTP error deleting employee id '{}': {}", id, ex.getMessage());
            throw new EmployeeServiceException("Failed to delete employee. HTTP Status: " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            log.error("EmployeeService : Unexpected error deleting employee id'{}': {}", id, ex.getMessage());
            throw new EmployeeServiceException(
                    "EmployeeService : Unexpected error occurred while deleting employee.", ex);
        }
    }

    public Employee fallbackGetEmployee(String id, Throwable t) {
        log.warn("Fallback triggered for getEmployeeById (id={}): {}", id, t.getMessage());
        throw new EmployeeServiceException("Fallback: Unable to fetch employee with ID: " + id, t);
    }

    public List<Employee> fallbackGetAllEmployees(Throwable t) {
        log.warn("Fallback triggered for getAllEmployees: {}", t.getMessage());
        return Collections.emptyList(); // Or throw if you prefer failure
    }

    public Employee fallbackCreateEmployee(EmployeeRequest request, Throwable t) {
        log.warn("Fallback triggered for createEmployee (name={}): {}", request.getName(), t.getMessage());
        throw new EmployeeServiceException("Fallback: Unable to create employee " + request.getName(), t);
    }

    public void fallbackDeleteEmployee(String id, Throwable t) {
        log.warn("Fallback triggered for deleteEmployeeById (id={}): {}", id, t.getMessage());
        throw new EmployeeServiceException("Fallback: Unable to delete employee with ID: " + id, t);
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
