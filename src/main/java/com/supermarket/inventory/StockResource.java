//package com.supermarket.inventory;
//import javax.ws.rs.*;
//import javax.ws.rs.core.*;
//import java.sql.*;
//import org.json.JSONArray;
//import org.json.JSONObject;
//@Path("/stocks")
//public class StockResource {
//
//    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory";
//    private static final String DB_USER = "root";
//    private static final String DB_PASS = "Seshu@0512";
//
//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response viewStocks(@QueryParam("id") Integer id) throws ClassNotFoundException
//    {
//        Class.forName("com.mysql.cj.jdbc.Driver");
//        try(Connection conn = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS))
//        {
//            if(id!=null)
//            {
//                String sql = "SELECT s.stock_id,s.product_id,p.product_name,s.available_quantity,s.last_updated "+
//                        "from stocks s join products p on s.product_id=p.product_id "+
//                        "where s.product_id=?";
//                PreparedStatement st = conn.prepareStatement(sql);
//                st.setInt(1,id);
//                ResultSet rs = st.executeQuery();
//                if(rs.next())
//                {
//                    JSONObject stock = new JSONObject();
//                    stock.put("stock_id",rs.getInt("stock_id"));
//                    stock.put("product_id",rs.getInt("product_id"));
//                    stock.put("product_name",rs.getString("product_name"));
//                    stock.put("available_quantity",rs.getInt("available_quantity"));
//                    stock.put("last_updated",rs.getDate("last_updated").toString());
//                    return Response.ok(stock.toString()).build();
//                }
//                else
//                {
//                    return Response.status(404).entity("{\"error\":\"Product not found\"}").build();
//                }
//            }
//            else
//            {
//                String sql = "SELECT * FROM stocks";
//                JSONArray stocks = new JSONArray();
//                PreparedStatement st = conn.prepareStatement(sql);
//                ResultSet rs = st.executeQuery();
//                while(rs.next())
//                {
//                    JSONObject stock = new JSONObject();
//                    stock.put("stock_id",rs.getInt("stock_id"));
//                    stock.put("product_id",rs.getInt("product_id"));
//                    stock.put("available_quantity",rs.getInt("available_quantity"));
//                    stock.put("last_updated",rs.getDate("last_updated").toString());
//                    stocks.put(stock);
//                }
//                return Response.ok(stocks.toString()).build();
//            }
//        }
//        catch (SQLException e)
//        {
//            e.printStackTrace();
//            return Response.status(500).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
//        }
//    }
//}
