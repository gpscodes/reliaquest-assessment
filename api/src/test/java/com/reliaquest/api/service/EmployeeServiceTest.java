package com.reliaquest.api.service;

import static com.reliaquest.api.utils.EmployeeConstants.MOCK_API_URL;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.InvalidEmployeeIdException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeApiResponse;
import com.reliaquest.api.model.EmployeeRequest;
import com.reliaquest.api.service.impl.EmployeeService;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    RestTemplate restTemplate;

    @Mock
    private WebClient webClient;

    @Mock
    private RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<WebClient.RequestBodySpec> requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

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

        EmployeeApiResponse<List<Employee>> mockResponse = new EmployeeApiResponse<>();
        mockResponse.setData(List.of(employee));
        mockResponse.setStatus("Successfully processed request.");
        ResponseEntity<EmployeeApiResponse<List<Employee>>> response =
                new ResponseEntity<>(mockResponse, HttpStatus.OK);

        // When
        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<List<Employee>>>>any()))
                .thenReturn(response);
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
        ResponseEntity<EmployeeApiResponse<List<Employee>>> response =
                new ResponseEntity<>(mockResponse, HttpStatus.OK);

        // When
        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<List<Employee>>>>any()))
                .thenReturn(response);
        List<Employee> result = employeeService.getAllEmployees();

        // Then
        assertEquals(0, result.size());
    }

    @Test
    public void testGetEmployeeById_returnsEmployee() {
        // Given
        mockGetEmployeeById();

        // When
        Employee result = employeeService.getEmployeeById(mockedEmployees.get(0).getId());

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
        // When
        HttpClientErrorException exception =
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null);

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<Employee>>>any()))
                .thenThrow(exception);

        // Then
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById("id"));
    }

    @Test
    void getEmployeeById_429RateLimit_throwsEmployeeServiceException() {
        // Given
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, null, null);

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<Employee>>>any()))
                .thenThrow(exception);

        // When & Then
        EmployeeServiceException ex =
                assertThrows(EmployeeServiceException.class, () -> employeeService.getEmployeeById("id"));
        assertTrue(ex.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    public void testGetEmployeesByNameSearch_returnsMatchingEmployees() {
        // Given
        mockGetAllEmployees();

        // When
        List<Employee> result = employeeService.getEmployeesByNameSearch("Prasad");

        // Then
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
        // Given
        mockGetAllEmployees();

        // When
        Integer max = employeeService.getHighestSalaryOfEmployees();

        // Then
        assertEquals(1100, max);
    }

    @Test
    void getTop10HighestEarningEmployeeNames_shouldReturnTop10SortedNames() {
        // Given
        List<Employee> mockEmployees = IntStream.range(1, 20)
                .mapToObj(i -> {
                    Employee e = new Employee();
                    e.setId(UUID.randomUUID().toString());
                    e.setEmployee_name("Employee " + i);
                    e.setEmployee_salary(1000 * i); // Increasing salaries
                    return e;
                })
                .collect(Collectors.toList());

        EmployeeApiResponse response = new EmployeeApiResponse();
        response.setData(mockEmployees);
        ResponseEntity<EmployeeApiResponse<Employee>> mockResponse = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<Employee>>>any()))
                .thenReturn(mockResponse);

        // When
        List<String> result = employeeService.getTop10HighestEarningEmployeeNames();

        // Then
        assertEquals(10, result.size());
        assertEquals("Employee 19", result.get(0)); // Highest salary
        assertEquals("Employee 10", result.get(9)); // 10th highest
    }

    @Test
    public void testCreateEmployee_returnsCreatedEmployee() {
        // Given
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

        EmployeeApiResponse<Employee> apiResponse = new EmployeeApiResponse<>();
        apiResponse.setStatus("success");
        apiResponse.setData(created);
        ResponseEntity<EmployeeApiResponse<Employee>> mockResponse = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<Employee>>>any()))
                .thenReturn(mockResponse);

        // When
        Employee result = employeeService.createEmployee(input);

        // Then
        assertNotNull(result);
        assertEquals("3", result.getId());
        assertEquals("Guru Developer", result.getEmployee_name());
        assertEquals("gurup@company.com", result.getEmployee_email());
    }

    @Test
    void createEmployee_nullResponseBody_throwsEmployeeServiceException() {
        // Given
        EmployeeRequest input = new EmployeeRequest();
        input.setName("Guru Developer");
        ResponseEntity<EmployeeApiResponse<Employee>> emptyResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<Employee>>>any()))
                .thenReturn(emptyResponse);

        //When & Then
        EmployeeServiceException ex = assertThrows(EmployeeServiceException.class, () -> {
            employeeService.createEmployee(input);
        });

        assertTrue(ex.getMessage().contains("empty response body"));
    }

    @Test
    void createEmployee_restClientException_throwsEmployeeServiceException() {
        // Given
        EmployeeRequest input = new EmployeeRequest();
        input.setName("Guru Developer");
        when(restTemplate.exchange(
                        eq(MOCK_API_URL),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<Employee>>>any()))
                .thenThrow(new RestClientException("Service unavailable"));

        // When & Then
        EmployeeServiceException result = assertThrows(EmployeeServiceException.class, () -> {
            employeeService.createEmployee(input);
        });
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

    private void mockGetEmployeeById() {
        EmployeeApiResponse<Employee> response = new EmployeeApiResponse<>();
        response.setData(mockedEmployees.get(0));
        ResponseEntity<EmployeeApiResponse<Employee>> mockResponse = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<Employee>>>any()))
                .thenReturn(mockResponse);
    }

    private String mockGetAllEmployees() {
        EmployeeApiResponse<List<Employee>> response = new EmployeeApiResponse<>();
        response.setData(mockedEmployees);
        final String employeeId = mockedEmployees.get(0).getId();
        ResponseEntity<EmployeeApiResponse<List<Employee>>> mockResponse =
                new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<EmployeeApiResponse<List<Employee>>>>any()))
                .thenReturn(mockResponse);
        return employeeId;
    }
}
