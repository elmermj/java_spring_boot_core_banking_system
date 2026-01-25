package com.aethelcbs.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    // Paths excluded from detailed logging
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/actuator/health",
        "/actuator/info"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        long startTime = System.currentTimeMillis();
        
        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log request details
            if (shouldLog(path)) {
                logRequest(wrappedRequest, wrappedResponse, duration);
            }
            
            // Copy response body back to original response
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    private boolean shouldLog(String path) {
        return EXCLUDED_PATHS.stream().noneMatch(path::startsWith);
    }
    
    private void logRequest(ContentCachingRequestWrapper request, 
                           ContentCachingResponseWrapper response, 
                           long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        int status = response.getStatus();
        String clientIp = getClientIpAddress(request);
        
        logger.info("{} {}?{} - {} - {}ms - IP: {}", 
            method, 
            uri, 
            queryString != null ? queryString : "", 
            status, 
            duration,
            clientIp);
        
        // Log errors
        if (status >= 400) {
            logger.warn("Request failed: {} {} - Status: {} - Duration: {}ms", 
                method, uri, status, duration);
        }
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
}
