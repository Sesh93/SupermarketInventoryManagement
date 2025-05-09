package com.supermarket.inventory;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
@Path("/products")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
public class ProductResource
{
    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";
    @GET
    public Response getProducts(@QueryParam("id") Integer id,
                                @QueryParam("category_id") Integer categoryId,
                                @QueryParam("purchase_date") String purchaseDate,
                                @QueryParam("sales_date") String salesDate) throws ClassNotFoundException
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS))
        {
            if (id != null)
            {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM products WHERE product_id = ?");
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                {
                    JSONObject product = new JSONObject();
                    product.put("product_id", rs.getInt("product_id"));
                    product.put("product_name", rs.getString("product_name"));
                    product.put("category_id", rs.getInt("category_id"));
                    product.put("price", rs.getDouble("price"));
                    product.put("unit", rs.getString("unit"));
                    return Response.ok(product.toString()).build();
                }
                else
                {
                    return Response.status(404).entity(new ErrorResponse("Product not found!")).build();
                }
            }
            else
            {
                StringBuilder query = new StringBuilder(
                        "SELECT DISTINCT p.product_id, p.product_name, p.category_id, c.category_name, p.price, p.unit " +
                                "FROM products p " +
                                "JOIN category c ON p.category_id = c.category_id ");
                List<String> conditions = new ArrayList<>();
                if (purchaseDate != null)
                {
                    query.append("JOIN purchase_items pi ON pi.product_id = p.product_id " +
                            "JOIN purchases pu ON pi.purchase_id = pu.purchase_id ");
                    conditions.add("DATE(pu.purchase_date) = ?");
                }
                if (salesDate != null)
                {
                    query.append("JOIN sale_item si ON si.product_id = p.product_id " +
                            "JOIN sales s ON si.sale_id = s.sale_id ");
                    conditions.add("DATE(s.sale_date) = ?");
                }
                if (categoryId != null)
                {
                    conditions.add("p.category_id = ?");
                }
                if (!conditions.isEmpty())
                {
                    query.append("WHERE ").append(String.join(" AND ", conditions));
                }
                PreparedStatement stmt = conn.prepareStatement(query.toString());
                int index = 1;
                if (purchaseDate != null) stmt.setDate(index++, java.sql.Date.valueOf(purchaseDate));
                if (salesDate != null) stmt.setDate(index++, java.sql.Date.valueOf(salesDate));
                if (categoryId != null) stmt.setInt(index, categoryId);
                ResultSet rs = stmt.executeQuery();
                JSONArray productList = new JSONArray();
                while (rs.next())
                {
                    JSONObject product = new JSONObject();
                    product.put("product_id", rs.getInt("product_id"));
                    product.put("product_name", rs.getString("product_name"));
                    product.put("category_id", rs.getInt("category_id"));
                    product.put("category_name", rs.getString("category_name"));
                    product.put("price", rs.getDouble("price"));
                    product.put("unit", rs.getString("unit"));
                    productList.put(product);
                }
                return Response.ok(productList.toString()).build();
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
            return Response.status(500).entity(new ErrorResponse("Failed to retrieve products!")).build();
        }
    }
    @GET
    @Path("/stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response viewStocks(@QueryParam("id") Integer id) throws ClassNotFoundException
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try(Connection conn = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS))
        {
            if(id!=null)
            {
                String sql = "SELECT s.stock_id,s.product_id,p.product_name,s.available_quantity,s.last_updated "+
                        "from stocks s join products p on s.product_id=p.product_id "+
                        "where s.product_id=?";
                PreparedStatement st = conn.prepareStatement(sql);
                st.setInt(1,id);
                ResultSet rs = st.executeQuery();
                if(rs.next())
                {
                    JSONObject stock = new JSONObject();
                    stock.put("stock_id",rs.getInt("stock_id"));
                    stock.put("product_id",rs.getInt("product_id"));
                    stock.put("product_name",rs.getString("product_name"));
                    stock.put("available_quantity",rs.getInt("available_quantity"));
                    stock.put("last_updated",rs.getDate("last_updated").toString());
                    return Response.ok(stock.toString()).build();
                }
                else
                {
                    return Response.status(404).entity("{\"error\":\"Product not found\"}").build();
                }
            }
            else
            {
                String sql = "SELECT * FROM stocks";
                JSONArray stocks = new JSONArray();
                PreparedStatement st = conn.prepareStatement(sql);
                ResultSet rs = st.executeQuery();
                while(rs.next())
                {
                    JSONObject stock = new JSONObject();
                    stock.put("stock_id",rs.getInt("stock_id"));
                    stock.put("product_id",rs.getInt("product_id"));
                    stock.put("available_quantity",rs.getInt("available_quantity"));
                    stock.put("last_updated",rs.getDate("last_updated").toString());
                    stocks.put(stock);
                }
                return Response.ok(stocks.toString()).build();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return Response.status(500).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addProduct(String body) throws ClassNotFoundException {
        JSONObject json = new JSONObject(body);
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String insertProductSQL = "INSERT INTO products (product_name, category_id, price, unit) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertProductSQL, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, json.getString("product_name"));
                stmt.setInt(2, json.getInt("category_id"));
                stmt.setDouble(3, json.getDouble("price"));
                stmt.setString(4, json.getString("unit"));
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int productId = rs.getInt(1);
                    int quantity = json.optInt("available_quantity", 0); // optional field in JSON
                    java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                    String checkStockSQL = "SELECT available_quantity FROM stocks WHERE product_id = ?";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkStockSQL)) {
                        checkStmt.setInt(1, productId);
                        ResultSet rsStock = checkStmt.executeQuery();
                        if (rsStock.next()) {
                            // Product exists in stock, update quantity
                            String updateStockSQL = "UPDATE stocks SET available_quantity = available_quantity + ? WHERE product_id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateStockSQL)) {
                                updateStmt.setInt(1, quantity);
                                updateStmt.setInt(2, productId);
                                updateStmt.executeUpdate();
                            }
                        } else {
                            String insertStockSQL = "INSERT INTO stocks (product_id, available_quantity, last_updated) VALUES (?, ?, ?)";
                            try (PreparedStatement stockStmt = conn.prepareStatement(insertStockSQL)) {
                                stockStmt.setInt(1, productId);
                                stockStmt.setInt(2, quantity);
                                stockStmt.setDate(3, today);
                                stockStmt.executeUpdate();
                            }
                        }
                    }
                    String checkPriceSQL = "SELECT * FROM price WHERE product_id = ? AND end_date IS NULL";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkPriceSQL)) {
                        checkStmt.setInt(1, productId);
                        ResultSet priceRs = checkStmt.executeQuery();

                        if (!priceRs.next()) {
                            String insertPriceSQL = "INSERT INTO price (product_id, price, start_date, end_date) VALUES (?, ?, ?, NULL)";
                            try (PreparedStatement priceStmt = conn.prepareStatement(insertPriceSQL)) {
                                priceStmt.setInt(1, productId);
                                priceStmt.setDouble(2, json.getDouble("price"));
                                priceStmt.setDate(3, today);
                                priceStmt.executeUpdate();
                            }
                        } else {
                            String updatePriceSQL = "UPDATE price SET end_date = ? WHERE product_id = ? AND end_date IS NULL";
                            try (PreparedStatement updatePriceStmt = conn.prepareStatement(updatePriceSQL)) {
                                updatePriceStmt.setDate(1, today);
                                updatePriceStmt.setInt(2, productId);
                                updatePriceStmt.executeUpdate();
                            }

                            String insertPriceSQL = "INSERT INTO price (product_id, price, start_date, end_date) VALUES (?, ?, ?, NULL)";
                            try (PreparedStatement priceStmt = conn.prepareStatement(insertPriceSQL)) {
                                priceStmt.setInt(1, productId);
                                priceStmt.setDouble(2, json.getDouble("price"));
                                priceStmt.setDate(3, today);
                                priceStmt.executeUpdate();
                            }
                        }
                    }

                    return Response.status(201).entity("{\"message\":\"Product, stock, and price added successfully\"}").build();
                } else {
                    return Response.status(500).entity(new ErrorResponse("Product ID retrieval failed")).build();
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            return Response.status(400).entity(new ErrorResponse("Invalid category_id (foreign key)")).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(500).entity(new ErrorResponse("Failed to add product, stock, and price")).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateProduct(@PathParam("id") int id, String body) throws ClassNotFoundException {
        JSONObject json = new JSONObject(body);
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            String updateProductSQL = "UPDATE products SET product_name=?, category_id=?, price=?, unit=? WHERE product_id=?";
            try (PreparedStatement stmt = conn.prepareStatement(updateProductSQL)) {
                stmt.setString(1, json.getString("product_name"));
                stmt.setInt(2, json.getInt("category_id"));
                stmt.setDouble(3, json.getDouble("price"));
                stmt.setString(4, json.getString("unit"));
                stmt.setInt(5, id);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    String checkPriceSQL = "SELECT * FROM price WHERE product_id = ? AND end_date IS NULL";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkPriceSQL)) {
                        checkStmt.setInt(1, id);
                        ResultSet priceRs = checkStmt.executeQuery();

                        if (!priceRs.next()) {
                            String insertPriceSQL = "INSERT INTO price (product_id, price, start_date, end_date) VALUES (?, ?, ?, NULL)";
                            try (PreparedStatement priceStmt = conn.prepareStatement(insertPriceSQL)) {
                                priceStmt.setInt(1, id);
                                priceStmt.setDouble(2, json.getDouble("price"));
                                priceStmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
                                priceStmt.executeUpdate();
                            }
                        } else {
                            String updatePriceSQL = "UPDATE price SET end_date = ? WHERE product_id = ? AND end_date IS NULL";
                            try (PreparedStatement updatePriceStmt = conn.prepareStatement(updatePriceSQL)) {
                                updatePriceStmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                                updatePriceStmt.setInt(2, id);
                                updatePriceStmt.executeUpdate();
                            }

                            String insertPriceSQL = "INSERT INTO price (product_id, price, start_date, end_date) VALUES (?, ?, ?, NULL)";
                            try (PreparedStatement priceStmt = conn.prepareStatement(insertPriceSQL)) {
                                priceStmt.setInt(1, id);
                                priceStmt.setDouble(2, json.getDouble("price"));
                                priceStmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
                                priceStmt.executeUpdate();
                            }
                        }
                    }

                    return Response.ok("{\"message\":\"Product and price updated successfully\"}").build();
                } else {
                    return Response.status(404).entity(new ErrorResponse("Product not found")).build();
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            return Response.status(400).entity(new ErrorResponse("Invalid category_id (foreign key)")).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(500).entity(new ErrorResponse("Failed to update product and price")).build();
        }
    }
}