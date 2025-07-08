package com.reliaquest.api.service;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeRequest;
import java.util.List;

public interface IEmployeeService {

    public List<Employee> getAllEmployees();

    public Employee getEmployeeById(String id);

    public List<Employee> getEmployeesByNameSearch(String name);

    public Integer getHighestSalaryOfEmployees();

    public Employee createEmployee(EmployeeRequest request);

    public List<String> getTop10HighestEarningEmployeeNames();

    public void deleteEmployeeById(String id);
}
