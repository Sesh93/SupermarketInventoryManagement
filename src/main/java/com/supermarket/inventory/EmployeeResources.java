package com.supermarket.inventory;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;

import org.json.*;
import java.util.*;
@Path("/employees")
public class EmployeeResources {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmployees(@QueryParam("id") Integer employeeId,
                                 @QueryParam("department_id") Integer departmentId) throws ClassNotFoundException {

        JSONArray employeesArray = new JSONArray();
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        StringBuilder sql = new StringBuilder("SELECT * FROM employee");
        List<Object> params = new ArrayList<>();

        String delimiter = " WHERE ";
        if (employeeId != null) {
            sql.append(delimiter).append("employee_id = ?");
            delimiter = " AND ";
            params.add(employeeId);
        }
        if (departmentId != null) {
            sql.append(delimiter).append("department_id = ?");
            params.add(departmentId);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject emp = new JSONObject();
                emp.put("employee_id", rs.getInt("employee_id"));
                emp.put("employee_name", rs.getString("employee_name"));
                emp.put("phone_no", rs.getString("phone_no"));
                emp.put("email_id", rs.getString("email_id"));
                emp.put("designation", rs.getString("designation"));
                emp.put("department_id", rs.getInt("department_id"));
                emp.put("password", rs.getString("password"));
                employeesArray.put(emp);
            }
            if(employeesArray.length()==1)
                return Response.ok(employeesArray.getJSONObject(0).toString()).build();
            return Response.ok(employeesArray.toString()).build();

        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }

    @GET
    @Path("/top_performers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopPerformers(@QueryParam("limit") @DefaultValue("5") int limit) throws ClassNotFoundException {
        JSONArray topEmployees = new JSONArray();
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sql = "SELECT e.employee_id, e.employee_name, SUM(s.total_amount) AS total_sales " +
                "FROM employee e " +
                "JOIN sales s ON e.employee_id = s.employee_id " +
                "GROUP BY e.employee_id " +
                "ORDER BY total_sales DESC " +
                "LIMIT ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject emp = new JSONObject();
                emp.put("employee_id", rs.getInt("employee_id"));
                emp.put("employee_name", rs.getString("employee_name"));
                emp.put("total_sales", rs.getDouble("total_sales"));
                topEmployees.put(emp);
            }
            return Response.ok(topEmployees.toString()).build();
        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addEmployee(String data) throws ClassNotFoundException {
        JSONObject empJson = new JSONObject(data);
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sql = "INSERT INTO employee (employee_name, phone_no, email_id, designation, department_id, password) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, empJson.getString("employee_name"));
            stmt.setString(2, empJson.getString("phone_no"));
            stmt.setString(3, empJson.getString("email_id"));
            stmt.setString(4, empJson.getString("designation"));
            stmt.setInt(5, empJson.getInt("department_id"));
            stmt.setString(6, empJson.getString("password"));

            int inserted = stmt.executeUpdate();
            if (inserted > 0) {
                responseJson.put("message", "Employee added successfully");
                return Response.status(Response.Status.CREATED).entity(responseJson.toString()).build();
            } else {
                responseJson.put("error", "Failed to add employee");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEmployee(@PathParam("id") int employeeId, String data) throws ClassNotFoundException {
        JSONObject empJson = new JSONObject(data);
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sql = "UPDATE employee SET employee_name=?, phone_no=?, email_id=?, designation=?, department_id=?, password=? WHERE employee_id=?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, empJson.getString("employee_name"));
            stmt.setString(2, empJson.getString("phone_no"));
            stmt.setString(3, empJson.getString("email_id"));
            stmt.setString(4, empJson.getString("designation"));
            stmt.setInt(5, empJson.getInt("department_id"));
            stmt.setString(6, empJson.getString("password"));
            stmt.setInt(7, employeeId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                responseJson.put("message", "Employee updated successfully");
                return Response.ok(responseJson.toString()).build();
            } else {
                responseJson.put("error", "Employee with ID " + employeeId + " not found");
                return Response.status(Response.Status.NOT_FOUND).entity(responseJson.toString()).build();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }
}
