package com.aethelcbs.api_gateway.filter;

import com.aethelcbs.api_gateway.service.JwtValidationService;
import com.aethelcbs.api_gateway.service.RateLimitService;
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
public class RateLimitFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private final RateLimitService rateLimitService;
    private final JwtValidationService jwtValidationService;
    
    // Paths excluded from rate limiting
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/actuator/"
    );
    
    public RateLimitFilter(RateLimitService rateLimitService, 
                          JwtValidationService jwtValidationService) {
        this.rateLimitService = rateLimitService;
        this.jwtValidationService = jwtValidationService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip rate limiting for excluded paths
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Determine rate limit key (prefer account ID from token, fallback to IP)
        String rateLimitKey = getRateLimitKey(request);
        
        if (!rateLimitService.isAllowed(rateLimitKey)) {
            int remaining = rateLimitService.getRemainingRequests(rateLimitKey);
            sendRateLimitResponse(response, remaining);
            return;
        }
        
        // Add rate limit headers
        int remaining = rateLimitService.getRemainingRequests(rateLimitKey);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Limit", String.valueOf(20)); // Default limit
        
        filterChain.doFilter(request, response);
    }
    
    private String getRateLimitKey(HttpServletRequest request) {
        // Try to extract account ID from JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (jwtValidationService.validateToken(token)) {
                    UUID accountId = jwtValidationService.extractAccountId(token);
                    return accountId.toString();
                }
            } catch (Exception e) {
                logger.debug("Could not extract account ID from token, using IP", e);
            }
        }
        
        // Fallback to IP address
        String ipAddress = getClientIpAddress(request);
        return "ip:" + ipAddress;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private void sendRateLimitResponse(HttpServletResponse response, int remaining) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\",\"retryAfter\":60}");
    }
}
