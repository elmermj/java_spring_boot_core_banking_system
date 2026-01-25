package com.aethelcbs.notification_service.service;

import com.aethelcbs.notification_service.dto.PushNotificationRequest;
import com.aethelcbs.notification_service.dto.PushNotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Unified push notification service that abstracts FCM and APNS
 * Provides a single interface for sending push notifications across platforms
 */
@Service
public class UnifiedPushNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedPushNotificationService.class);
    
    private final List<PushNotificationProvider> providers;
    private final RetryablePushService retryablePushService;
    
    public UnifiedPushNotificationService(List<PushNotificationProvider> providers,
                                         RetryablePushService retryablePushService) {
        this.providers = providers;
        this.retryablePushService = retryablePushService;
    }
    
    /**
     * Send push notification using the appropriate provider based on platform
     * Automatically selects FCM for Android, APNS for iOS
     */
    public CompletableFuture<PushNotificationResult> sendNotification(
            PushNotificationRequest request) {
        
        // Determine platform if not specified
        if (request.getPlatform() == null || request.getPlatform().isEmpty()) {
            // Try to infer from push service
            if (request.getPushService() != null) {
                request.setPlatform("FCM".equals(request.getPushService()) ? "ANDROID" : "IOS");
            } else {
                logger.warn("Platform not specified in request");
                return CompletableFuture.completedFuture(
                    PushNotificationResult.failure(request.getDeviceToken(), 
                        "Platform not specified"));
            }
        }
        
        // Find appropriate provider
        PushNotificationProvider provider = findProvider(request);
        
        if (provider == null) {
            logger.error("No enabled provider found for platform: {}", request.getPlatform());
            return CompletableFuture.completedFuture(
                PushNotificationResult.failure(request.getDeviceToken(), 
                    "No enabled provider for platform: " + request.getPlatform()));
        }
        
        // Set push service name
        request.setPushService(provider.getServiceName());
        
        // Send with retry logic
        return retryablePushService.sendWithRetry(provider, request);
    }
    
    /**
     * Send notifications to multiple devices
     */
    public Map<String, CompletableFuture<PushNotificationResult>> sendBulkNotifications(
            List<PushNotificationRequest> requests) {
        
        return requests.stream()
            .collect(Collectors.toMap(
                PushNotificationRequest::getDeviceToken,
                this::sendNotification
            ));
    }
    
    /**
     * Find the appropriate provider for the request
     */
    private PushNotificationProvider findProvider(PushNotificationRequest request) {
        String platform = request.getPlatform().toUpperCase();
        
        return providers.stream()
            .filter(PushNotificationProvider::isEnabled)
            .filter(p -> platform.equals(p.getSupportedPlatform()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get status of all providers
     */
    public Map<String, Boolean> getProviderStatus() {
        return providers.stream()
            .collect(Collectors.toMap(
                PushNotificationProvider::getServiceName,
                PushNotificationProvider::isEnabled
            ));
    }
}
