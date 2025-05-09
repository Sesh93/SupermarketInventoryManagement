package com.supermarket.inventory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import org.json.*;

@Path("/customers")
public class CustomerResources {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomers(@QueryParam("id") Integer customerId) throws ClassNotFoundException {
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            if (customerId != null) {
                String sql = "SELECT * FROM customers WHERE customer_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, customerId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        responseJson.put("customer_id", rs.getInt("customer_id"));
                        responseJson.put("customer_name", rs.getString("customer_name"));
                        responseJson.put("phone_no", rs.getString("phone_no"));
                        responseJson.put("email_id", rs.getString("email_id"));
                        return Response.ok(responseJson.toString()).build();
                    } else {
                        responseJson.put("error", "Customer with ID " + customerId + " not found");
                        return Response.status(Response.Status.NOT_FOUND).entity(responseJson.toString()).build();
                    }
                }
            } else {
                JSONArray customers = new JSONArray();
                String sql = "SELECT * FROM customers";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        JSONObject customerJson = new JSONObject();
                        customerJson.put("customer_id", rs.getInt("customer_id"));
                        customerJson.put("customer_name", rs.getString("customer_name"));
                        customerJson.put("phone_no", rs.getString("phone_no"));
                        customerJson.put("email_id", rs.getString("email_id"));
                        customers.put(customerJson);
                    }
                }
                return Response.ok(customers.toString()).build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addCustomer(String customerData) throws ClassNotFoundException {
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            JSONObject customerJson = new JSONObject(customerData);

            String sql = "INSERT INTO customers (customer_name, phone_no, email_id) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, customerJson.getString("customer_name"));
                stmt.setString(2, customerJson.getString("phone_no"));
                stmt.setString(3, customerJson.getString("email_id"));

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    responseJson.put("message", "Customer added successfully");
                    return Response.status(Response.Status.CREATED).entity(responseJson.toString()).build();
                } else {
                    responseJson.put("error", "Failed to add customer");
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
                }
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
    public Response updateCustomer(@PathParam("id") int customerId, String customerData) throws ClassNotFoundException {
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            JSONObject customerJson = new JSONObject(customerData);

            String checkExistenceSql = "SELECT * FROM customers WHERE customer_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkExistenceSql)) {
                stmt.setInt(1, customerId);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    responseJson.put("error", "Customer with ID " + customerId + " does not exist");
                    return Response.status(Response.Status.BAD_REQUEST).entity(responseJson.toString()).build();
                }
            }

            String sql = "UPDATE customers SET customer_name = ?, phone_no = ?, email_id = ? WHERE customer_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, customerJson.getString("customer_name"));
                stmt.setString(2, customerJson.getString("phone_no"));
                stmt.setString(3, customerJson.getString("email_id"));
                stmt.setInt(4, customerId);

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    return Response.ok("{\"message\": \"Customer details updated successfully\"}").build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Customer with ID " + customerId + " not found\"}")
                            .build();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }
}
