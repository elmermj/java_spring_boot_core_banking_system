package com.aethelcbs.notification_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aethelcbs.notification_service.dto.PushNotificationRequest;
import com.aethelcbs.notification_service.dto.PushNotificationResult;
import com.aethelcbs.notification_service.entity.DevicePushToken;
import com.aethelcbs.notification_service.repository.DevicePushTokenRepository;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final DevicePushTokenRepository devicePushTokenRepository;
    private final UnifiedPushNotificationService unifiedPushService;
    
    public NotificationService(DevicePushTokenRepository devicePushTokenRepository,
                               UnifiedPushNotificationService unifiedPushService) {
        this.devicePushTokenRepository = devicePushTokenRepository;
        this.unifiedPushService = unifiedPushService;
    }
    
    /**
     * Send logout notification to a device
     * Uses unified push service with retry logic
     */
    public void sendLogoutNotification(String deviceId, String accountNumber) {
        try {
            // Find active push tokens for this device
            List<DevicePushToken> tokens = devicePushTokenRepository.findByDeviceIdAndIsActiveTrue(deviceId);
            
            if (tokens.isEmpty()) {
                logger.debug("No push tokens found for device: {}", deviceId);
                return;
            }
            
            String title = "Session Terminated";
            String body = "You have been logged out from another device. Please login again.";
            
            for (DevicePushToken token : tokens) {
                PushNotificationRequest request = new PushNotificationRequest();
                request.setDeviceToken(token.getPushToken());
                request.setPlatform(token.getPlatform());
                request.setPushService(token.getPushService());
                request.setTitle(title);
                request.setBody(body);
                request.setNotificationType("logout");
                
                // Send with retry logic (async)
                unifiedPushService.sendNotification(request)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            logger.info("Sent {} logout notification to device: {}", 
                                token.getPushService(), deviceId);
                        } else {
                            logger.warn("Failed to send {} notification to device: {} - {}", 
                                token.getPushService(), deviceId, result.getErrorMessage());
                            
                            // Mark token as inactive if invalid
                            if (result.isTokenInvalid()) {
                                markTokenInactive(token.getId());
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("Exception sending notification to device: {}", deviceId, ex);
                        return null;
                    });
            }
        } catch (Exception e) {
            logger.error("Error sending logout notification to device: {}", deviceId, e);
        }
    }
    
    /**
     * Mark token as inactive (called when token is invalid)
     */
    @Transactional
    private void markTokenInactive(UUID tokenId) {
        try {
            Optional<DevicePushToken> tokenOpt = devicePushTokenRepository.findById(tokenId);
            if (tokenOpt.isPresent()) {
                DevicePushToken token = tokenOpt.get();
                token.setIsActive(false);
                devicePushTokenRepository.save(token);
                logger.info("Marked push token as inactive: {}", tokenId);
            }
        } catch (Exception e) {
            logger.error("Error marking token as inactive: {}", tokenId, e);
        }
    }
    
    /**
     * Send logout notifications to multiple devices
     */
    public void sendLogoutNotifications(List<String> deviceIds, String accountNumber) {
        deviceIds.forEach(deviceId -> sendLogoutNotification(deviceId, accountNumber));
    }
    
    /**
     * Register or update a device push token (idempotent)
     */
    @Transactional
    public void registerPushToken(UUID userId, String deviceId, String pushToken, 
                                  String platform) {
        try {
            // Determine push service based on platform
            String pushService = "ANDROID".equalsIgnoreCase(platform) ? "FCM" : "APNS";
            
            // Check if token already exists (idempotent check)
            Optional<DevicePushToken> existingTokenOpt = devicePushTokenRepository
                .findByDeviceIdAndUserIdAndPlatform(deviceId, userId, platform.toUpperCase());
            
            if (existingTokenOpt.isPresent()) {
                // Update existing token (idempotent - only if changed)
                DevicePushToken existingToken = existingTokenOpt.get();
                if (!existingToken.getPushToken().equals(pushToken) || !existingToken.getIsActive()) {
                    existingToken.setPushToken(pushToken);
                    existingToken.setIsActive(true);
                    devicePushTokenRepository.save(existingToken);
                    logger.info("Updated push token for device: {}, platform: {}", deviceId, platform);
                } else {
                    logger.debug("Push token already up to date for device: {}, platform: {}", deviceId, platform);
                }
            } else {
                // Create new token
                DevicePushToken newToken = new DevicePushToken();
                newToken.setDeviceId(deviceId);
                newToken.setUserId(userId);
                newToken.setPushToken(pushToken);
                newToken.setPlatform(platform.toUpperCase());
                newToken.setPushService(pushService);
                newToken.setIsActive(true);
                devicePushTokenRepository.save(newToken);
                logger.info("Registered new push token for device: {}, platform: {}", deviceId, platform);
            }
        } catch (Exception e) {
            logger.error("Error registering push token", e);
            throw new RuntimeException("Failed to register push token", e);
        }
    }
}
