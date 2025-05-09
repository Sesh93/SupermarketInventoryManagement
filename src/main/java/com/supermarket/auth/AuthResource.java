package com.supermarket.auth;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;

@Path("/auth")
public class AuthResource {
    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/inventory";
        String user = "root"; // Change if needed
        String password = "Seshu@0512"; // Change if needed
        return DriverManager.getConnection(url, user, password);
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(User loginUser) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = getConnection()) {
            String sql = "SELECT email_id, password, designation FROM employee WHERE email_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, loginUser.getEmail());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String dbPass = rs.getString("password");
                String roleStr = rs.getString("designation").toUpperCase();
                if (dbPass.equals(loginUser.getPassword())) {
                    Role role;
                    try {
                        role = Role.valueOf(roleStr);
                    } catch (IllegalArgumentException e) {
                        return Response.status(Response.Status.UNAUTHORIZED)
                                .entity("{\"error\":\"Invalid role: " + roleStr + "\"}")
                                .build();
                    }

                    String token = JwtUtil.generateToken(loginUser.getEmail(), role);
                    return Response.ok("{\"token\":\"" + token + "\"}").build();
                }
            }

            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid email or password\"}")
                    .build();

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Database error\"}")
                    .build();
        }
    }
}
