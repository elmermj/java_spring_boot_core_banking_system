package com.aethelcbs.notification_service.service;

import com.aethelcbs.notification_service.dto.PushNotificationRequest;
import com.aethelcbs.notification_service.dto.PushNotificationResult;

/**
 * Unified interface for push notification providers (FCM, APNS)
 */
public interface PushNotificationProvider {
    
    /**
     * Send a push notification
     * @param request The notification request
     * @return Result indicating success or failure
     */
    PushNotificationResult sendNotification(PushNotificationRequest request);
    
    /**
     * Check if this provider is enabled and ready
     */
    boolean isEnabled();
    
    /**
     * Get the platform this provider supports (ANDROID/IOS)
     */
    String getSupportedPlatform();
    
    /**
     * Get the service name (FCM/APNS)
     */
    String getServiceName();
}
