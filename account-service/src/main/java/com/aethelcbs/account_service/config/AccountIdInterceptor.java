package com.aethelcbs.account_service.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class AccountIdInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip for actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            return true;
        }
        
        // Skip for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        String accountIdHeader = request.getHeader("X-Account-Id");
        
        if (accountIdHeader == null || accountIdHeader.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"X-Account-Id header is required\"}");
            return false;
        }
        
        try {
            UUID.fromString(accountIdHeader);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid X-Account-Id format\"}");
            return false;
        }
        
        return true;
    }
}
