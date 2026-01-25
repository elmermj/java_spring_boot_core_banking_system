package com.aethelcbs.notification_service.service;

import com.aethelcbs.notification_service.dto.PushNotificationRequest;
import com.aethelcbs.notification_service.dto.PushNotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Production-grade retry service with exponential backoff for push notifications
 */
@Service
public class RetryablePushService {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryablePushService.class);
    
    @Value("${app.push.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${app.push.retry.initial-delay-ms:1000}")
    private long initialDelayMs;
    
    @Value("${app.push.retry.max-delay-ms:30000}")
    private long maxDelayMs;
    
    @Value("${app.push.retry.backoff-multiplier:2.0}")
    private double backoffMultiplier;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    
    /**
     * Execute push notification with retry logic
     */
    public CompletableFuture<PushNotificationResult> sendWithRetry(
            PushNotificationProvider provider,
            PushNotificationRequest request) {
        
        CompletableFuture<PushNotificationResult> future = new CompletableFuture<>();
        
        sendWithRetryInternal(provider, request, 0, future);
        
        return future;
    }
    
    private void sendWithRetryInternal(
            PushNotificationProvider provider,
            PushNotificationRequest request,
            int attemptNumber,
            CompletableFuture<PushNotificationResult> future) {
        
        if (attemptNumber >= maxRetryAttempts) {
            logger.error("Max retry attempts ({}) exceeded for device token: {}", 
                maxRetryAttempts, request.getDeviceToken());
            future.complete(PushNotificationResult.failure(
                request.getDeviceToken(), 
                "Max retry attempts exceeded"));
            return;
        }
        
        try {
            PushNotificationResult result = provider.sendNotification(request);
            result.setRetryCount(attemptNumber);
            
            if (result.isSuccess()) {
                if (attemptNumber > 0) {
                    logger.info("Push notification succeeded after {} retry attempts for token: {}", 
                        attemptNumber, request.getDeviceToken());
                }
                future.complete(result);
                return;
            }
            
            // Check if token is invalid - don't retry
            if (result.isTokenInvalid()) {
                logger.warn("Device token is invalid, not retrying: {}", request.getDeviceToken());
                future.complete(result);
                return;
            }
            
            // Check if error is retryable
            if (!isRetryableError(result.getErrorMessage())) {
                logger.warn("Non-retryable error, not retrying: {}", result.getErrorMessage());
                future.complete(result);
                return;
            }
            
            // Calculate delay with exponential backoff
            long delay = calculateBackoffDelay(attemptNumber);
            logger.warn("Push notification failed (attempt {}/{}), retrying in {}ms: {}", 
                attemptNumber + 1, maxRetryAttempts, delay, result.getErrorMessage());
            
            // Schedule retry
            scheduler.schedule(() -> {
                sendWithRetryInternal(provider, request, attemptNumber + 1, future);
            }, delay, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            logger.error("Exception during push notification attempt {}: {}", 
                attemptNumber + 1, request.getDeviceToken(), e);
            
            if (attemptNumber + 1 >= maxRetryAttempts) {
                future.complete(PushNotificationResult.failure(
                    request.getDeviceToken(), 
                    "Exception after " + (attemptNumber + 1) + " attempts: " + e.getMessage()));
            } else {
                long delay = calculateBackoffDelay(attemptNumber);
                scheduler.schedule(() -> {
                    sendWithRetryInternal(provider, request, attemptNumber + 1, future);
                }, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    private boolean isRetryableError(String errorMessage) {
        if (errorMessage == null) {
            return true; // Retry on unknown errors
        }
        
        // Non-retryable errors
        String lowerError = errorMessage.toLowerCase();
        if (lowerError.contains("invalid") && 
            (lowerError.contains("token") || lowerError.contains("registration"))) {
            return false; // Invalid token - don't retry
        }
        
        if (lowerError.contains("unregistered") || lowerError.contains("not-registered")) {
            return false; // Unregistered token - don't retry
        }
        
        if (lowerError.contains("authentication") || lowerError.contains("unauthorized")) {
            return false; // Auth errors - don't retry
        }
        
        // Retryable errors (network issues, timeouts, server errors)
        return true;
    }
    
    private long calculateBackoffDelay(int attemptNumber) {
        // Exponential backoff: initialDelay * (multiplier ^ attemptNumber)
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber));
        
        // Cap at max delay
        return Math.min(delay, maxDelayMs);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
