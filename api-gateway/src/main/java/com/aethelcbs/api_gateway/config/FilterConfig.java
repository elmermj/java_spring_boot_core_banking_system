package com.aethelcbs.api_gateway.config;

import com.aethelcbs.api_gateway.filter.AccountIdValidationFilter;
import com.aethelcbs.api_gateway.filter.AuthenticationFilter;
import com.aethelcbs.api_gateway.filter.RateLimitFilter;
import com.aethelcbs.api_gateway.filter.RequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {
    
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(
            RequestLoggingFilter filter) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
    
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilter(
            AuthenticationFilter filter) {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
    
    @Bean
    public FilterRegistrationBean<AccountIdValidationFilter> accountIdValidationFilter(
            AccountIdValidationFilter filter) {
        FilterRegistrationBean<AccountIdValidationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        return registration;
    }
}
