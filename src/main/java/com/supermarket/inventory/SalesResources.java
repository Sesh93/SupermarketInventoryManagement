package com.supermarket.inventory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import org.json.*;
import java.util.*;

@Path("/sales")
public class SalesResources {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSales(@QueryParam("id") Integer id,
                             @QueryParam("customer_id") Integer customerId,
                             @QueryParam("employee_id") Integer employeeId) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        JSONObject errorJson = new JSONObject();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            if (id != null) return getSaleById(conn, id);
            else if (customerId != null) return getSalesByCustomer(conn, customerId);
            else if (employeeId != null) return getSalesByEmployee(conn, employeeId);
            else return getAllSales(conn);
        } catch (SQLException e) {
            return Response.status(500).entity(new ErrorResponse("Database error occurred")).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSale(String saleData) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            JSONObject saleJson = new JSONObject(saleData);
            if (saleJson.has("customer")) {
                int newCustomerId = createCustomer(conn, saleJson.getJSONObject("customer"));
                if (newCustomerId == -1) return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Customer creation failed\"}").build();
                saleJson.put("customer_id", newCustomerId);
            } else if (!saleJson.has("customer_id") || !customerExists(conn, saleJson.getInt("customer_id"))) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Valid customer info is required\"}").build();
            }
            return createSaleInternal(conn, saleJson);
        } catch (Exception e) {
            return Response.status(500).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }


    private Response createSaleInternal(Connection conn, JSONObject saleJson) throws SQLException {
        int customerId = saleJson.getInt("customer_id");
        int employeeId = saleJson.getInt("employee_id");
        String paymentMethod = saleJson.getString("payment_method");
        JSONArray items = saleJson.getJSONArray("items");

        Map<Integer, Integer> stockAvailability = getStockAvailability(conn, items);
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            int productId = item.getInt("product_id");
            int quantity = item.getInt("quantity");
            if (stockAvailability.getOrDefault(productId, 0) < quantity) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Insufficient stock for product " + productId + "\"}").build();
            }
        }

        double totalAmount = 0.0;
        String insertSaleSQL = "INSERT INTO sales (customer_id, employee_id, sale_date, total_amount, payment_method) VALUES (?, ?, CURDATE(), ?, ?)";
        try (PreparedStatement saleStmt = conn.prepareStatement(insertSaleSQL, Statement.RETURN_GENERATED_KEYS)) {
            saleStmt.setInt(1, customerId);
            saleStmt.setInt(2, employeeId);
            saleStmt.setDouble(3, totalAmount);
            saleStmt.setString(4, paymentMethod);
            saleStmt.executeUpdate();

            ResultSet keys = saleStmt.getGeneratedKeys();
            if (!keys.next()) return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Sale creation failed\"}").build();
            int saleId = keys.getInt(1);

            try (PreparedStatement itemStmt = conn.prepareStatement("INSERT INTO sale_item (sale_id, product_id, quantity, price_per_unit, total_price) VALUES (?, ?, ?, ?, ?)");
                 PreparedStatement stockStmt = conn.prepareStatement("UPDATE stocks SET available_quantity = available_quantity - ?, last_updated = CURDATE() WHERE product_id = ?")) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    int productId = item.getInt("product_id");
                    int quantity = item.getInt("quantity");
                    double unitPrice = getProductPrice(conn, productId);
                    double totalPrice = unitPrice * quantity;
                    totalAmount += totalPrice;

                    itemStmt.setInt(1, saleId);
                    itemStmt.setInt(2, productId);
                    itemStmt.setInt(3, quantity);
                    itemStmt.setDouble(4, unitPrice);
                    itemStmt.setDouble(5, totalPrice);
                    itemStmt.executeUpdate();

                    stockStmt.setInt(1, quantity);
                    stockStmt.setInt(2, productId);
                    stockStmt.executeUpdate();
                }
            }

            try (PreparedStatement updateTotal = conn.prepareStatement("UPDATE sales SET total_amount = ? WHERE sale_id = ?")) {
                updateTotal.setDouble(1, totalAmount);
                updateTotal.setInt(2, saleId);
                updateTotal.executeUpdate();
            }

            return Response.status(Response.Status.CREATED).entity("{\"message\":\"Sale created\",\"sale_id\":" + saleId + "}").build();
        }
    }

    private boolean customerExists(Connection conn, int customerId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM customers WHERE customer_id = ?")) {
            stmt.setInt(1, customerId);
            return stmt.executeQuery().next();
        }
    }

    private int createCustomer(Connection conn, JSONObject cust) throws SQLException {
        String sql = "INSERT INTO customers (customer_name, phone_no, email_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cust.getString("customer_name"));
            stmt.setString(2, cust.getString("phone_no"));
            stmt.setString(3, cust.optString("email_id", ""));
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private double getProductPrice(Connection conn, int productId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT price FROM price WHERE product_id = ? and end_date is null")) {
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("price") : -1;
        }
    }

    private Map<Integer, Integer> getStockAvailability(Connection conn, JSONArray items) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        if (items.isEmpty()) return map;

        StringBuilder q = new StringBuilder("SELECT product_id, available_quantity FROM stocks WHERE product_id IN (");
        for (int i = 0; i < items.length(); i++) {
            q.append("?");
            if (i < items.length() - 1) q.append(",");
        }
        q.append(")");

        try (PreparedStatement stmt = conn.prepareStatement(q.toString())) {
            for (int i = 0; i < items.length(); i++) {
                stmt.setInt(i + 1, items.getJSONObject(i).getInt("product_id"));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) map.put(rs.getInt("product_id"), rs.getInt("available_quantity"));
        }

        return map;
    }

    private Response getAllSales(Connection conn) throws SQLException {
        JSONArray array = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sales");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) array.put(buildSaleJson(conn, rs));
        }
        return Response.ok(array.toString()).build();
    }

    private Response getSaleById(Connection conn, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sales WHERE sale_id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? Response.ok(buildSaleJson(conn, rs).toString()).build()
                    : Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Sale not found")).build();
        }
    }

    private Response getSalesByCustomer(Connection conn, int customerId) throws SQLException {
        JSONArray array = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sales WHERE customer_id = ?")) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) array.put(buildSaleJson(conn, rs));
        }
        return Response.ok(array.toString()).build();
    }

    private Response getSalesByEmployee(Connection conn, int employeeId) throws SQLException {
        JSONArray array = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sales WHERE employee_id = ?")) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) array.put(buildSaleJson(conn, rs));
        }
        return Response.ok(array.toString()).build();
    }

    private JSONObject buildSaleJson(Connection conn, ResultSet rs) throws SQLException {
        JSONObject obj = new JSONObject();
        int saleId = rs.getInt("sale_id");
        obj.put("sale_id", saleId);
        obj.put("customer_id", rs.getInt("customer_id"));
        obj.put("employee_id", rs.getInt("employee_id"));
        obj.put("sale_date", rs.getDate("sale_date"));
        obj.put("total_amount", rs.getDouble("total_amount"));
        obj.put("payment_method", rs.getString("payment_method"));

        JSONArray items = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sale_item WHERE sale_id = ?")) {
            stmt.setInt(1, saleId);
            ResultSet rsItems = stmt.executeQuery();
            while (rsItems.next()) {
                JSONObject item = new JSONObject();
                item.put("product_id", rsItems.getInt("product_id"));
                item.put("quantity", rsItems.getInt("quantity"));
                item.put("price_per_unit", rsItems.getDouble("price_per_unit"));
                item.put("total_price", rsItems.getDouble("total_price"));
                items.put(item);
            }
        }
        obj.put("items", items);
        return obj;
    }
}
