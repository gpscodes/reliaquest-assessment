package com.reliaquest.api;

import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.EmployeeRequest;
import com.reliaquest.api.domain.EmployeeResponse;
import com.reliaquest.api.service.EmployeeService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.NoSuchElementException;

import static com.reliaquest.api.utils.EmployeeConstants.MOCK_API_URL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
class ApiApplicationTest {

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
    void someTest() {
        //Given
        Employee employee = new Employee();
        employee.setEmployee_name("Test Employee");

        EmployeeResponse mockResponse = new EmployeeResponse();
        mockResponse.setData(List.of(employee));
        mockResponse.setStatus("Successfully processed request.");

        //When
        when(restTemplate.getForObject(anyString(), eq(EmployeeResponse.class)))
                .thenReturn(mockResponse);
        List<Employee> result = employeeService.getAllEmployees();

        //Then
        assertEquals(1, result.size());
        assertEquals("Test Employee", result.get(0).getEmployee_name());
    }

    @Test
    public void testGetEmployeeById_returnsEmployee() {
        EmployeeResponse response = new EmployeeResponse();
        response.setData(mockedEmployees);
        final String employeeId = mockedEmployees.get(0).getId();

        when(restTemplate.getForObject(contains(employeeId), eq(EmployeeResponse.class)))
                .thenReturn(response);

        Employee result = employeeService.getEmployeeById(employeeId);
        assertEquals("Guru Prasad", result.getEmployee_name());
    }

    @Test
    public void testGetEmployeesByNameSearch_returnsMatchingEmployees() {
        EmployeeResponse response = new EmployeeResponse();
        response.setData(mockedEmployees);

        when(restTemplate.getForObject(anyString(), eq(EmployeeResponse.class)))
                .thenReturn(response);

        // Act
        List<Employee> result = employeeService.getEmployeesByNameSearch("Prasad");

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getEmployee_name().equals("Guru Prasad")));

    }

    @Test
    public void testGetHighestSalaryOfEmployees_returnsMaxSalary() {
        EmployeeResponse response = new EmployeeResponse();
        response.setData(mockedEmployees);

        when(restTemplate.getForObject(anyString(), eq(EmployeeResponse.class)))
                .thenReturn(response);

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

        EmployeeResponse response = new EmployeeResponse();
        response.setData(List.of(created));

        // Mocking RestTemplate
        when(restTemplate.postForEntity(
                eq("http://localhost:8112/api/v1/employee"),
                any(HttpEntity.class),
                eq(EmployeeResponse.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.CREATED));

        // Act
        Employee result = employeeService.createEmployee(input);

        // Assert
        assertNotNull(result);
        assertEquals("3", result.getId());
        assertEquals("Guru Developer", result.getEmployee_name());
        assertEquals("gurup@company.com", result.getEmployee_email());
    }

    @Test
    void deleteEmployeeById_shouldReturnName_whenEmployeeIsDeleted() {
        // Arrange
        String employeeId = "1";
        String employeeName = "Guru Prasad";

        // Mock getEmployeeById to return a valid employee
        EmployeeResponse response = new EmployeeResponse();
        response.setData(mockedEmployees);

        when(restTemplate.getForObject(contains(employeeId), eq(EmployeeResponse.class)))
                .thenReturn(response);

        // Mock delete to do nothing (success)
        doNothing().when(restTemplate).delete(MOCK_API_URL + "/" + employeeName);

        // Act
        String deletedName = employeeService.deleteEmployeeById(employeeId);

        // Assert
        assertEquals(employeeName, deletedName);
    }

    @Test
    void deleteEmployeeById_shouldThrowNotFound_whenEmployeeIsMissing() {
        // Arrange
        String employeeId = "404-id";
        Employee missingEmployee = new Employee();
        missingEmployee.setEmployee_name("Ghost Person");

        // Mock getEmployeeById to return the ghost employee
        EmployeeResponse response = new EmployeeResponse();
        response.setData(List.of(missingEmployee));

        when(restTemplate.getForObject(contains(employeeId), eq(EmployeeResponse.class)))
                .thenReturn(response);

        // Mock DELETE call to throw 404
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(restTemplate)
                .delete(MOCK_API_URL + "/Ghost Person");

        // Act + Assert
        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> employeeService.deleteEmployeeById(employeeId));

        assertTrue(thrown.getMessage().contains("Unable to delete"));
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
