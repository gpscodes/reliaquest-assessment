package com.reliaquest.api.service;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.InvalidEmployeeIdException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeApiResponse;
import com.reliaquest.api.model.EmployeeRequest;
import com.reliaquest.api.model.SingleEmployeeApiResponse;
import com.reliaquest.api.service.impl.EmployeeService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class EmployeeServiceTest {

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    EmployeeService employeeService;

    private static List<Employee> mockedEmployees;

    @BeforeAll
    static void init() {
        mockedEmployees = getMockedEmployee();
    }

    @Test
    void testGetAllEmployees_returnsListOfEmployees() {
        // Given
        Employee employee = new Employee();
        employee.setEmployee_name("Test Employee");

        EmployeeApiResponse mockResponse = new EmployeeApiResponse();
        mockResponse.setData(List.of(employee));
        mockResponse.setStatus("Successfully processed request.");

        // When
        when(restTemplate.getForObject(anyString(), eq(EmployeeApiResponse.class))).thenReturn(mockResponse);
        List<Employee> result = employeeService.getAllEmployees();

        // Then
        assertEquals(1, result.size());
        assertEquals("Test Employee", result.get(0).getEmployee_name());
    }

    @Test
    void testGetAllEmployees_returnsEmptyList() {
        // Given
        EmployeeApiResponse mockResponse = new EmployeeApiResponse();
        mockResponse.setData(Collections.emptyList());
        mockResponse.setStatus("Successfully processed request.");

        // When
        when(restTemplate.getForObject(anyString(), eq(EmployeeApiResponse.class))).thenReturn(mockResponse);
        List<Employee> result = employeeService.getAllEmployees();

        // Then
        assertEquals(0, result.size());
    }

    @Test
    public void testGetEmployeeById_returnsEmployee() {
        // Given
        SingleEmployeeApiResponse response = new SingleEmployeeApiResponse();
        response.setData(mockedEmployees.get(0));
        final String employeeId = mockedEmployees.get(0).getId();
        ResponseEntity<SingleEmployeeApiResponse> mockResponse =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()))
                .thenReturn(mockResponse);

        // When
        Employee result = employeeService.getEmployeeById(employeeId);

        // Then
        assertNotNull(result);
        assertEquals("Guru Prasad", result.getEmployee_name());
    }

    @Test
    public void testGetEmployeeByIdWithEmptyInput_throwsException() {
        // When & Then
        assertThrows(InvalidEmployeeIdException.class, () -> employeeService.getEmployeeById(""));
    }

    @Test
    void testGetEmployeeById_404NotFound_throwsEmployeeNotFoundException() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                null
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()
        )).thenThrow(exception);

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById("id"));
    }

    @Test
    void getEmployeeById_429RateLimit_throwsEmployeeServiceException() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                null,
                null
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()
        )).thenThrow(exception);

        EmployeeServiceException ex = assertThrows(EmployeeServiceException.class, () -> employeeService.getEmployeeById("id"));
        assertTrue(ex.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    public void testGetEmployeesByNameSearch_returnsMatchingEmployees() {
        EmployeeApiResponse response = new EmployeeApiResponse();
        response.setData(mockedEmployees);

        when(restTemplate.getForObject(anyString(), eq(EmployeeApiResponse.class))).thenReturn(response);

        // Act
        List<Employee> result = employeeService.getEmployeesByNameSearch("Prasad");

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getEmployee_name().equals("Guru Prasad")));
    }

    @Test
    public void testGetEmployeesByNameSearchWithEmptyName_throwsException() {
        // When
        List<Employee> result = employeeService.getEmployeesByNameSearch("");

        // Then
        assertEquals(0, result.size());
    }

    @Test
    public void testGetHighestSalaryOfEmployees_returnsMaxSalary() {
        EmployeeApiResponse response = new EmployeeApiResponse();
        response.setData(mockedEmployees);

        when(restTemplate.getForObject(anyString(), eq(EmployeeApiResponse.class))).thenReturn(response);

        Integer max = employeeService.getHighestSalaryOfEmployees();
        assertEquals(1100, max);
    }

    @Test
    public void testCreateEmployee_returnsCreatedEmployee() {
        // Arrange: input employee (creation request)
        EmployeeRequest input = new EmployeeRequest();
        input.setName("Guru Developer");
        input.setSalary(1500);
        input.setAge(30);
        input.setTitle("Software Engineer");

        // Expected response from API
        Employee created = new Employee();
        created.setId("3");
        created.setEmployee_name("Guru Developer");
        created.setEmployee_salary(1500);
        created.setEmployee_age(30);
        created.setEmployee_title("Software Engineer");
        created.setEmployee_email("gurup@company.com");

        EmployeeApiResponse response = new EmployeeApiResponse();
        response.setData(List.of(created));

        // Mocking RestTemplate
        when(restTemplate.postForEntity(
                eq("http://localhost:8112/api/v1/employee"), any(HttpEntity.class), eq(Employee.class)))
                .thenReturn(new ResponseEntity<>(created, HttpStatus.CREATED));

        // Act
        Employee result = employeeService.createEmployee(input);

        // Assert
        assertNotNull(result);
        assertEquals("3", result.getId());
        assertEquals("Guru Developer", result.getEmployee_name());
        assertEquals("gurup@company.com", result.getEmployee_email());
    }

    @Test
    void createEmployee_nullResponseBody_throwsEmployeeServiceException() {
        EmployeeRequest input = new EmployeeRequest();
        input.setName("Guru Developer");
        ResponseEntity<Employee> response = new ResponseEntity<>(null, HttpStatus.CREATED);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Employee.class)
        )).thenReturn(response);

        EmployeeServiceException ex = assertThrows(EmployeeServiceException.class, () -> {
            employeeService.createEmployee(input);
        });

        assertTrue(ex.getMessage().contains("empty response body"));
    }

    @Test
    void createEmployee_restClientException_throwsEmployeeServiceException() {
        EmployeeRequest input = new EmployeeRequest();
        input.setName("Guru Developer");
        RestClientException ex = new RestClientException("Connection error");

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Employee.class)
        )).thenThrow(ex);

        EmployeeServiceException result = assertThrows(EmployeeServiceException.class, () -> {
            employeeService.createEmployee(input);
        });

        assertEquals("Failed to create employee due to API error", result.getMessage());
        assertEquals(ex, result.getCause());
    }


    @Test
    void deleteEmployeeById_success() {
        // Mock getEmployeeById
        SingleEmployeeApiResponse response = new SingleEmployeeApiResponse();
        response.setData(mockedEmployees.get(0));
        ResponseEntity<SingleEmployeeApiResponse> mockResponseById =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()))
                .thenReturn(mockResponseById);

        // Mock delete exchange
        Map<String, Object> mockResponseBody = new HashMap<>();
        mockResponseBody.put("data", true);
        mockResponseBody.put("status", "success");

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Map.class))
        ).thenReturn(mockResponse);

        assertDoesNotThrow(() -> employeeService.deleteEmployeeById(mockedEmployees.get(0).getId()));
    }

    @Test
    void deleteEmployeeById_employeeNotFound() {
        SingleEmployeeApiResponse response = new SingleEmployeeApiResponse();
        ResponseEntity<SingleEmployeeApiResponse> mockResponseById =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()))
                .thenReturn(mockResponseById);

        assertThrows(EmployeeServiceException.class, () -> employeeService.deleteEmployeeById("id"));
    }

    @Test
    void deleteEmployeeById_missingName_throwsException() {
        SingleEmployeeApiResponse response = new SingleEmployeeApiResponse();
        Employee employee = new Employee();
        employee.setEmployee_name(null);
        response.setData(employee);
        ResponseEntity<SingleEmployeeApiResponse> mockResponseById =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()))
                .thenReturn(mockResponseById);

        EmployeeServiceException ex = assertThrows(EmployeeServiceException.class, () -> employeeService.deleteEmployeeById("id"));
        assertTrue(ex.getMessage().contains("name is missing"));
    }

    @Test
    void deleteEmployeeById_apiReturnsFalse_throwsException() {
        SingleEmployeeApiResponse response = new SingleEmployeeApiResponse();
        response.setData(mockedEmployees.get(0));
        ResponseEntity<SingleEmployeeApiResponse> mockResponseById =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()))
                .thenReturn(mockResponseById);

        Map<String, Object> mockResponseBody = new HashMap<>();
        mockResponseBody.put("data", false);

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Map.class))
        ).thenReturn(mockResponse);

        assertThrows(EmployeeServiceException.class, () -> employeeService.deleteEmployeeById(mockedEmployees.get(0).getId()));
    }

    @Test
    void deleteEmployeeById_rateLimited_throwsException() {
        SingleEmployeeApiResponse response = new SingleEmployeeApiResponse();
        response.setData(mockedEmployees.get(0));
        ResponseEntity<SingleEmployeeApiResponse> mockResponseById =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()))
                .thenReturn(mockResponseById);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Map.class))
        ).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(EmployeeServiceException.class, () -> employeeService.deleteEmployeeById(mockedEmployees.get(0).getId()));
    }

    @Test
    void deleteEmployeeById_genericError_throwsException() {
        SingleEmployeeApiResponse response = new SingleEmployeeApiResponse();
        response.setData(mockedEmployees.get(0));
        ResponseEntity<SingleEmployeeApiResponse> mockResponseById =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<SingleEmployeeApiResponse>>any()))
                .thenReturn(mockResponseById);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Map.class))
        ).thenThrow(new RuntimeException("Unexpected"));

        assertThrows(EmployeeServiceException.class, () -> employeeService.deleteEmployeeById(mockedEmployees.get(0).getId()));
    }

    @Test
    void getTop10HighestEarningEmployeeNames_shouldReturnTop10SortedNames() {
        // Arrange
        List<Employee> mockEmployees = IntStream.range(1, 20)
                .mapToObj(i -> {
                    Employee e = new Employee();
                    e.setId(UUID.randomUUID().toString());
                    e.setEmployee_name("Employee " + i);
                    e.setEmployee_salary(1000 * i); // Increasing salaries
                    return e;
                })
                .collect(Collectors.toList());

        EmployeeApiResponse mockResponse = new EmployeeApiResponse();
        mockResponse.setData(mockEmployees);

        when(restTemplate.getForObject(anyString(), eq(EmployeeApiResponse.class))).thenReturn(mockResponse);

        // Act
        List<String> result = employeeService.getTop10HighestEarningEmployeeNames();

        // Assert
        assertEquals(10, result.size());
        assertEquals("Employee 19", result.get(0)); // Highest salary
        assertEquals("Employee 10", result.get(9)); // 10th highest
    }

    private static List<Employee> getMockedEmployee() {
        Employee mockEmp1 = new Employee();
        mockEmp1.setId("1");
        mockEmp1.setEmployee_name("Guru Prasad");
        mockEmp1.setEmployee_salary(1000);

        Employee mockEmp2 = new Employee();
        mockEmp2.setId("2");
        mockEmp2.setEmployee_name("Prasad Srinivasan");
        mockEmp2.setEmployee_salary(1100);

        return List.of(mockEmp1, mockEmp2);
    }
}
