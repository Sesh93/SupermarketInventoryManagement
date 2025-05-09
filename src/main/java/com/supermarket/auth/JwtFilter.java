package com.supermarket.auth;

import io.jsonwebtoken.Claims;
import javax.annotation.Priority;
import javax.ws.rs.container.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.ws.rs.Priorities;

@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            abort(requestContext, "Authorization header must be provided", Response.Status.UNAUTHORIZED);
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        try {
            Claims claims = JwtUtil.validateToken(token); // ensure JwtUtil exists
            String roleString = claims.get("role", String.class);

            Role userRole = Role.valueOf(roleString); // convert to enum

            Method method = resourceInfo.getResourceMethod();
            Secured secured = method.getAnnotation(Secured.class);
            if (secured == null) {
                secured = resourceInfo.getResourceClass().getAnnotation(Secured.class);
            }

            if (secured != null) {
                Role[] allowedRoles = secured.value();
                if (!Arrays.asList(allowedRoles).contains(userRole)) {
                    abort(requestContext, "Access denied for role: " + userRole, Response.Status.FORBIDDEN);
                }
            }

        } catch (IllegalArgumentException e) {
            abort(requestContext, "Invalid role in token", Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            abort(requestContext, "Invalid or expired token", Response.Status.UNAUTHORIZED);
        }
    }

    private void abort(ContainerRequestContext requestContext, String message, Response.Status status) {
        requestContext.abortWith(Response.status(status)
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}
