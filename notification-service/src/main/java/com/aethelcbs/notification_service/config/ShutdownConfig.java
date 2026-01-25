package com.aethelcbs.notification_service.config;

import com.aethelcbs.notification_service.service.RetryablePushService;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShutdownConfig {
    
    private final RetryablePushService retryablePushService;
    
    public ShutdownConfig(RetryablePushService retryablePushService) {
        this.retryablePushService = retryablePushService;
    }
    
    @PreDestroy
    public void shutdown() {
        retryablePushService.shutdown();
    }
}
