package com.supermarket.inventory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

@Path("/prices")
public class PriceResources {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPrices(@QueryParam("product_id") Integer productId,
                              @QueryParam("start_date") String startDate,
                              @QueryParam("end_date") String endDate,
                              @QueryParam("sort") String sort,
                              @QueryParam("limit") Integer limit,
                              @QueryParam("category_id") Integer categoryId) throws ClassNotFoundException {
        JSONArray pricesArray = new JSONArray();
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        StringBuilder sql = new StringBuilder("SELECT p.product_id, p.price, p.start_date ,p.end_date FROM price p ");
        List<Object> params = new ArrayList<>();

        String delimiter = " WHERE ";

        // Handle category filter
        if (categoryId != null) {
            sql.append(delimiter).append("p.product_id IN (SELECT product_id FROM products WHERE category_id = ?)");
            params.add(categoryId);
            delimiter = " AND ";
        }

        // Handle product_id filter
        if (productId != null) {
            sql.append(delimiter).append("p.product_id = ?");
            params.add(productId);
            delimiter = " AND ";
        }

        // Handle start_date filter
        if (startDate != null) {
            sql.append(delimiter).append("p.start_date >= ?");
            params.add(startDate);
            delimiter = " AND ";
        }

        // Handle end_date filter
        if (endDate != null) {
            sql.append(delimiter).append("p.end_date <= ?");
            params.add(endDate);
            delimiter = " AND ";
        }

        // Handle sorting if provided
        if (sort != null) {
            sql.append(" ORDER BY p.end_date ").append(sort.equalsIgnoreCase("desc") ? "DESC" : "ASC");
        }

        // Handle limiting the number of results if provided
        if (limit != null) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set the parameters dynamically based on the provided filters
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject price = new JSONObject();
                price.put("product_id", rs.getInt("product_id"));
                price.put("price", rs.getDouble("price"));
                price.put("start_date", rs.getDate("start_date"));
                price.put("end_date", rs.getDate("end_date"));
                pricesArray.put(price);
            }

            return Response.ok(pricesArray.toString()).build();

        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }

    // POST /price/{product_id}?start_date={start_date}
    @POST
    @Path("/{product_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPrice(@PathParam("product_id") int productId, String data) throws ClassNotFoundException {
        JSONObject priceJson = new JSONObject(data);
        JSONObject responseJson = new JSONObject();
        Class.forName("com.mysql.cj.jdbc.Driver");

        String startDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String updatePriceSql = "UPDATE price SET end_date = ? WHERE product_id = ? AND end_date IS NULL";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            try (PreparedStatement updateStmt = conn.prepareStatement(updatePriceSql)) {
                updateStmt.setString(1, startDate);
                updateStmt.setInt(2, productId);
                updateStmt.executeUpdate();
            }

            String insertPriceSql = "INSERT INTO price (product_id, price, start_date, end_date) VALUES (?, ?, ?, NULL)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertPriceSql)) {
                insertStmt.setInt(1, productId);
                insertStmt.setDouble(2, priceJson.getDouble("price"));
                insertStmt.setString(3, startDate);

                int inserted = insertStmt.executeUpdate();
                if (inserted > 0) {
                    responseJson.put("message", "Price added successfully");
                    return Response.status(Response.Status.CREATED).entity(responseJson.toString()).build();
                } else {
                    responseJson.put("error", "Failed to add price");
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            responseJson.put("error", "Database error occurred");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseJson.toString()).build();
        }
    }

}
