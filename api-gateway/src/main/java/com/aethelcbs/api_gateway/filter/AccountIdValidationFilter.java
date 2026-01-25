package com.aethelcbs.api_gateway.filter;

import com.aethelcbs.api_gateway.service.JwtValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class AccountIdValidationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountIdValidationFilter.class);
    
    private final JwtValidationService jwtValidationService;
    
    // Paths that don't require X-Account-Id header
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/api/auth/",
        "/api/notifications/",
        "/actuator/"
    );
    
    public AccountIdValidationFilter(JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip validation for excluded paths
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check if X-Account-Id header exists
        String accountIdHeader = request.getHeader("X-Account-Id");
        if (accountIdHeader == null || accountIdHeader.trim().isEmpty()) {
            sendBadRequestResponse(response, "X-Account-Id header is required");
            return;
        }
        
        // Validate account ID format
        try {
            UUID.fromString(accountIdHeader);
        } catch (IllegalArgumentException e) {
            sendBadRequestResponse(response, "Invalid X-Account-Id format");
            return;
        }
        
        // Validate that account ID matches JWT token (if token exists)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (jwtValidationService.validateToken(token)) {
                    UUID tokenAccountId = jwtValidationService.extractAccountId(token);
                    UUID headerAccountId = UUID.fromString(accountIdHeader);
                    
                    if (!tokenAccountId.equals(headerAccountId)) {
                        sendForbiddenResponse(response, "Account ID in header does not match token");
                        return;
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not validate account ID against token", e);
                // Continue - token validation is handled by AuthenticationFilter
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void sendBadRequestResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
    
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
