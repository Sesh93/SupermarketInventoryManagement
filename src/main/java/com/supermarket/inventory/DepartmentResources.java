package com.supermarket.inventory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;

import com.supermarket.auth.Role;
import com.supermarket.auth.Secured;
import org.json.*;

@Path("/departments")
@Secured({Role.ADMIN})
public class DepartmentResources {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDepartments(
            @QueryParam("id") Integer departmentId,
            @QueryParam("sort_by") @DefaultValue("") String sortBy,
            @QueryParam("order") @DefaultValue("asc") String order) throws ClassNotFoundException {

        JSONArray resultArray = new JSONArray();
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            if (departmentId != null) {
                String sql = "SELECT d.department_id, d.department_name, e.employee_id, e.employee_name " +
                        "FROM departments d " +
                        "LEFT JOIN employee e ON d.department_id = e.department_id " +
                        "WHERE d.department_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, departmentId);
                    ResultSet rs = stmt.executeQuery();

                    JSONObject dept = new JSONObject();
                    JSONArray employeesArray = new JSONArray();
                    boolean departmentExists = false;

                    while (rs.next()) {
                        if (!departmentExists) {
                            dept.put("department_id", rs.getInt("department_id"));
                            dept.put("department_name", rs.getString("department_name"));
                            departmentExists = true;
                        }
                        int empId = rs.getInt("employee_id");
                        if (!rs.wasNull()) {
                            JSONObject emp = new JSONObject();
                            emp.put("employee_id", empId);
                            emp.put("employee_name", rs.getString("employee_name"));
                            employeesArray.put(emp);
                        }
                    }

                    if (departmentExists) {
                        dept.put("employees", employeesArray);
                        resultArray.put(dept);
                    } else {
                        responseJson.put("error", "Department not found");
                        return Response.status(Response.Status.NOT_FOUND).entity(responseJson.toString()).build();
                    }
                }

            } else if ("employees".equalsIgnoreCase(sortBy)) {
                // Use Case 3: Retrieve all departments sorted by number of employees
                String sql = "SELECT d.department_id, d.department_name, COUNT(e.employee_id) AS num_employees " +
                        "FROM departments d " +
                        "LEFT JOIN employee e ON d.department_id = e.department_id " +
                        "GROUP BY d.department_id " +
                        "ORDER BY num_employees " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        JSONObject dept = new JSONObject();
                        dept.put("department_id", rs.getInt("department_id"));
                        dept.put("department_name", rs.getString("department_name"));
                        dept.put("num_employees", rs.getInt("num_employees"));
                        resultArray.put(dept);
                    }
                }

            } else {
                // Use Case 1: Retrieve all departments
                String sql = "SELECT department_id, department_name FROM departments";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        JSONObject dept = new JSONObject();
                        dept.put("department_id", rs.getInt("department_id"));
                        dept.put("department_name", rs.getString("department_name"));
                        resultArray.put(dept);
                    }
                }
            }

            return Response.ok(resultArray.toString()).build();

        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDepartment(String data) throws ClassNotFoundException {
        JSONObject deptJson = new JSONObject(data);
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sql = "INSERT INTO departments (department_name) VALUES (?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, deptJson.getString("department_name"));

            int inserted = stmt.executeUpdate();
            if (inserted > 0) {
                responseJson.put("message", "Department added successfully");
                return Response.status(Response.Status.CREATED).entity(responseJson.toString()).build();
            } else {
                responseJson.put("error", "Failed to add department");
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
    public Response updateDepartment(@PathParam("id") int departmentId, String data) throws ClassNotFoundException {
        JSONObject deptJson = new JSONObject(data);
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sql = "UPDATE departments SET department_name=? WHERE department_id=?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, deptJson.getString("department_name"));
            stmt.setInt(2, departmentId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                responseJson.put("message", "Department updated successfully");
                return Response.ok(responseJson.toString()).build();
            } else {
                responseJson.put("error", "Department with ID " + departmentId + " not found");
                return Response.status(Response.Status.NOT_FOUND).entity(responseJson.toString()).build();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }
}
