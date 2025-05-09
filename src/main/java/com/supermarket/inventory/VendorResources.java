package com.supermarket.inventory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import org.json.*;

@Path("/vendors")
public class VendorResources {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVendors(@QueryParam("id") Integer vendorId) throws ClassNotFoundException {
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            if (vendorId != null) {
                String sql = "SELECT * FROM vendors WHERE vendor_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, vendorId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        responseJson.put("vendor_id", rs.getInt("vendor_id"));
                        responseJson.put("vendor_name", rs.getString("vendor_name"));
                        responseJson.put("phone_no", rs.getString("phone_no"));
                        responseJson.put("email_id", rs.getString("email_id"));
                        return Response.ok(responseJson.toString()).build();
                    } else {
                        responseJson.put("error", "Vendor with ID " + vendorId + " not found");
                        return Response.status(Response.Status.NOT_FOUND).entity(responseJson.toString()).build();
                    }
                }
            } else {
                JSONArray vendors = new JSONArray();
                String sql = "SELECT * FROM vendors";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        JSONObject vendorJson = new JSONObject();
                        vendorJson.put("vendor_id", rs.getInt("vendor_id"));
                        vendorJson.put("vendor_name", rs.getString("vendor_name"));
                        vendorJson.put("phone_no", rs.getString("phone_no"));
                        vendorJson.put("email_id", rs.getString("email_id"));
                        vendors.put(vendorJson);
                    }
                }
                return Response.ok(vendors.toString()).build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(500).entity(new ErrorResponse("Database error occurred")).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addVendor(String vendorData) throws ClassNotFoundException {
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            JSONObject vendorJson = new JSONObject(vendorData);

            String sql = "INSERT INTO vendors (vendor_name, phone_no, email_id) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, vendorJson.getString("vendor_name"));
                stmt.setString(2, vendorJson.getString("phone_no"));
                stmt.setString(3, vendorJson.getString("email_id"));

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    responseJson.put("message", "Vendor added successfully");
                    return Response.status(Response.Status.CREATED).entity(responseJson.toString()).build();
                } else {
                    return Response.status(500).entity(new ErrorResponse("Failed to add vendor")).build();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(500).entity(new ErrorResponse("Database error occurred")).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVendor(@PathParam("id") int vendorId, String vendorData) throws ClassNotFoundException {
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            JSONObject vendorJson = new JSONObject(vendorData);

            String checkExistenceSql = "SELECT * FROM vendors WHERE vendor_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkExistenceSql)) {
                stmt.setInt(1, vendorId);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    responseJson.put("error", "Vendor with ID " + vendorId + " does not exist");
                    return Response.status(Response.Status.BAD_REQUEST).entity(responseJson.toString()).build();
                }
            }

            String sql = "UPDATE vendors SET vendor_name = ?, phone_no = ?, email_id = ? WHERE vendor_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, vendorJson.getString("vendor_name"));
                stmt.setString(2, vendorJson.getString("phone_no"));
                stmt.setString(3, vendorJson.getString("email_id"));
                stmt.setInt(4, vendorId);

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    return Response.ok("{\"message\": \"Vendor details updated successfully\"}").build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Vendor with ID " + vendorId + " not found\"}")
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
