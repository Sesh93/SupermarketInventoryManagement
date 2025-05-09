package com.supermarket.inventory;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource
{
    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Seshu@0512";
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategories(@QueryParam("id") Integer id) throws ClassNotFoundException
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS))
        {
            if (id != null)
            {
                // Get category by ID
                String sql = "SELECT * FROM category WHERE category_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                {
                    JSONObject category = new JSONObject();
                    category.put("category_id", rs.getInt("category_id"));
                    category.put("category_name", rs.getString("category_name"));
                    return Response.ok(category.toString(), MediaType.APPLICATION_JSON).build();
                }
                else
                {
                    return Response.status(404).entity(new ErrorResponse("Category not found")).build();
                }
            }
            else
            {
                // Get all categories
                String sql = "SELECT * FROM category";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                JSONArray categories = new JSONArray();
                while (rs.next())
                {
                    JSONObject category = new JSONObject();
                    category.put("category_id", rs.getInt("category_id"));
                    category.put("category_name", rs.getString("category_name"));
                    categories.put(category);
                }
                return Response.ok(categories.toString(), MediaType.APPLICATION_JSON).build();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return Response.status(500).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    @POST
    public Response addCategory(String jsonData) throws ClassNotFoundException
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try
        {
            JSONObject json = new JSONObject(jsonData);
            String categoryName = json.optString("category_name");
            if (categoryName == null || categoryName.trim().isEmpty())
            {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Category name is required\"}").build();
            }
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS))
            {
                String sql = "INSERT INTO category (category_name) VALUES (?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, categoryName);
                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0)
                {
                    return Response.status(Response.Status.CREATED).entity("{\"message\":\"Category added successfully\"}").build();
                }
                else
                {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Insertion failed\"}").build();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    @PUT
    @Path("/{id}")
    public Response updateCategory(@PathParam("id") int categoryId, String jsonData) throws ClassNotFoundException
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try {
            JSONObject json = new JSONObject(jsonData);
            String categoryName = json.optString("category_name");
            if (categoryName == null || categoryName.trim().isEmpty())
            {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Category name is required\"}").build();
            }
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS))
            {
                String sql = "UPDATE category SET category_name = ? WHERE category_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, categoryName);
                stmt.setInt(2, categoryId);

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0)
                {
                    return Response.ok("{\"message\":\"Category updated successfully\"}").build();
                }
                else
                {
                    return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"Category not found\"}").build();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
