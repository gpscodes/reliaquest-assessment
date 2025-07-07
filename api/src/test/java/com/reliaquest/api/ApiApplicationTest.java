package com.reliaquest.api;

import static com.reliaquest.api.utils.EmployeeConstants.MOCK_API_URL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeRequest;
import com.reliaquest.api.model.EmployeeApiResponse;
import com.reliaquest.api.service.impl.EmployeeService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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
    public void testGetEmployeeById_returnsEmployee() {
        EmployeeApiResponse response = new EmployeeApiResponse();
        response.setData(mockedEmployees);
        final String employeeId = mockedEmployees.get(0).getId();

        when(restTemplate.getForObject(contains(employeeId), eq(EmployeeApiResponse.class)))
                .thenReturn(response);

        Optional<Employee> result = Optional.ofNullable(employeeService.getEmployeeById(employeeId));
        assertEquals("Guru Prasad", result.get());
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
    void deleteEmployeeById_shouldReturnName_whenEmployeeIsDeleted() {
        // Arrange
        String employeeId = "1";
        String employeeName = "Guru Prasad";

        // Mock getEmployeeById to return a valid employee
        EmployeeApiResponse response = new EmployeeApiResponse();
        response.setData(mockedEmployees);

        when(restTemplate.getForObject(contains(employeeId), eq(EmployeeApiResponse.class)))
                .thenReturn(response);

        // Mock delete to do nothing (success)
        doNothing().when(restTemplate).delete(MOCK_API_URL + "/" + employeeName);

        // Act
        employeeService.deleteEmployeeById(employeeId);

        // Assert
        //        assertEquals(employeeName, deletedName);
    }

    @Test
    void deleteEmployeeById_shouldThrowNotFound_whenEmployeeIsMissing() {
        // Arrange
        String employeeId = "404-id";
        Employee missingEmployee = new Employee();
        missingEmployee.setEmployee_name("Ghost Person");

        // Mock getEmployeeById to return the ghost employee
        EmployeeApiResponse response = new EmployeeApiResponse();

        when(restTemplate.getForObject(contains(employeeId), eq(EmployeeApiResponse.class)))
                .thenReturn(response);

        // Act + Assert
        NoSuchElementException thrown =
                assertThrows(NoSuchElementException.class, () -> employeeService.deleteEmployeeById(employeeId));

        assertTrue(thrown.getMessage().contains("Employee not found for ID: 404-id"));
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
