package com.supermarket.inventory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import org.json.*;
import java.util.*;

@Path("/purchases")
public class PurchaseResources {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPurchases(@QueryParam("id") Integer id,
                                 @QueryParam("vendor_id") Integer vendorId,
                                 @QueryParam("employee_id") Integer employeeId) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        JSONObject errorJson = new JSONObject();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            if (id != null) return getPurchaseById(conn, id);
            else if (vendorId != null) return getPurchasesByVendor(conn, vendorId);
            else if (employeeId != null) return getPurchasesByEmployee(conn, employeeId);
            else return getAllPurchases(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse("Database error occurred")).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPurchase(String purchaseData) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            JSONObject purchaseJson = new JSONObject(purchaseData);
            if (purchaseJson.has("vendor")) {
                int newVendorId = createVendor(conn, purchaseJson.getJSONObject("vendor"));
                if (newVendorId == -1) return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("Vendor creation failed")).build();
                purchaseJson.put("vendor_id", newVendorId);
            } else if (!purchaseJson.has("vendor_id") || !vendorExists(conn, purchaseJson.getInt("vendor_id"))) {
                return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("Valid vendor info is required")).build();
            }
            return createPurchaseInternal(conn, purchaseJson);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    private Response createPurchaseInternal(Connection conn, JSONObject purchaseJson) throws SQLException {
        int vendorId = purchaseJson.getInt("vendor_id");
        int employeeId = purchaseJson.getInt("employee_id");
        String paymentMethod = purchaseJson.getString("payment_method");
        JSONArray items = purchaseJson.getJSONArray("items");

        double totalAmount = 0.0;
        String insertPurchaseSQL = "INSERT INTO purchases (vendor_id, employee_id, purchase_date, total_amount, payment_method) VALUES (?, ?, CURDATE(), ?, ?)";
        try (PreparedStatement purchaseStmt = conn.prepareStatement(insertPurchaseSQL, Statement.RETURN_GENERATED_KEYS)) {
            purchaseStmt.setInt(1, vendorId);
            purchaseStmt.setInt(2, employeeId);
            purchaseStmt.setDouble(3, totalAmount);
            purchaseStmt.setString(4, paymentMethod);
            purchaseStmt.executeUpdate();

            ResultSet keys = purchaseStmt.getGeneratedKeys();
            if (!keys.next()) return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse("Purchase creation failed")).build();
            int purchaseId = keys.getInt(1);

            try (PreparedStatement itemStmt = conn.prepareStatement("INSERT INTO purchase_items (purchase_id, product_id, quantity, price_per_unit, total_price) VALUES (?, ?, ?, ?, ?)");
                 PreparedStatement stockStmt = conn.prepareStatement("UPDATE stocks SET available_quantity = available_quantity + ?, last_updated = CURDATE() WHERE product_id = ?")) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    int productId = item.getInt("product_id");
                    int quantity = item.getInt("quantity");
                    double unitPrice = item.getDouble("price_per_unit");
                    double totalPrice = unitPrice * quantity;
                    totalAmount += totalPrice;

                    itemStmt.setInt(1, purchaseId);
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

            try (PreparedStatement updateTotal = conn.prepareStatement("UPDATE purchases SET total_amount = ? WHERE purchase_id = ?")) {
                updateTotal.setDouble(1, totalAmount);
                updateTotal.setInt(2, purchaseId);
                updateTotal.executeUpdate();
            }

            return Response.status(Response.Status.CREATED).entity("{\"message\":\"Purchase created\",\"purchase_id\":" + purchaseId + "}").build();
        }
    }

    private boolean vendorExists(Connection conn, int vendorId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM vendors WHERE vendor_id = ?")) {
            stmt.setInt(1, vendorId);
            return stmt.executeQuery().next();
        }
    }

    private int createVendor(Connection conn, JSONObject vendor) throws SQLException {
        String sql = "INSERT INTO vendors (vendor_name, phone_no, email_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, vendor.getString("vendor_name"));
            stmt.setString(2, vendor.getString("phone_no"));
            stmt.setString(3, vendor.optString("email_id", ""));
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private Response getAllPurchases(Connection conn) throws SQLException {
        JSONArray array = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM purchases");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) array.put(buildPurchaseJson(conn, rs));
        }
        return Response.ok(array.toString()).build();
    }

    private Response getPurchaseById(Connection conn, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM purchases WHERE purchase_id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? Response.ok(buildPurchaseJson(conn, rs).toString()).build()
                    : Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Purchase not found")).build();
        }
    }

    private Response getPurchasesByVendor(Connection conn, int vendorId) throws SQLException {
        JSONArray array = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM purchases WHERE vendor_id = ?")) {
            stmt.setInt(1, vendorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) array.put(buildPurchaseJson(conn, rs));
        }
        return Response.ok(array.toString()).build();
    }

    private Response getPurchasesByEmployee(Connection conn, int employeeId) throws SQLException {
        JSONArray array = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM purchases WHERE employee_id = ?")) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) array.put(buildPurchaseJson(conn, rs));
        }
        return Response.ok(array.toString()).build();
    }

    private JSONObject buildPurchaseJson(Connection conn, ResultSet rs) throws SQLException {
        JSONObject obj = new JSONObject();
        int purchaseId = rs.getInt("purchase_id");
        obj.put("purchase_id", purchaseId);
        obj.put("vendor_id", rs.getInt("vendor_id"));
        obj.put("employee_id", rs.getInt("employee_id"));
        obj.put("purchase_date", rs.getDate("purchase_date"));
        obj.put("total_amount", rs.getDouble("total_amount"));
        obj.put("payment_method", rs.getString("payment_method"));

        JSONArray items = new JSONArray();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM purchase_items WHERE purchase_id = ?")) {
            stmt.setInt(1, purchaseId);
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