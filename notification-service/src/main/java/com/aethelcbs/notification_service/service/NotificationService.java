package com.aethelcbs.notification_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aethelcbs.notification_service.entity.DevicePushToken;
import com.aethelcbs.notification_service.repository.DevicePushTokenRepository;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final DevicePushTokenRepository devicePushTokenRepository;
    private final FcmService fcmService;
    private final ApnsService apnsService;
    
    public NotificationService(DevicePushTokenRepository devicePushTokenRepository,
                               FcmService fcmService,
                               ApnsService apnsService) {
        this.devicePushTokenRepository = devicePushTokenRepository;
        this.fcmService = fcmService;
        this.apnsService = apnsService;
    }
    
    /**
     * Send logout notification to a device
     * Looks up the device's push token and sends via FCM (Android) or APNS (iOS)
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
                try {
                    if ("FCM".equals(token.getPushService()) && fcmService.isEnabled()) {
                        fcmService.sendNotification(token.getPushToken(), title, body);
                        logger.info("Sent FCM logout notification to device: {}", deviceId);
                    } else if ("APNS".equals(token.getPushService()) && apnsService.isEnabled()) {
                        apnsService.sendNotification(token.getPushToken(), title, body);
                        logger.info("Sent APNS logout notification to device: {}", deviceId);
                    } else {
                        logger.debug("Push service {} not enabled or not initialized for device: {}", 
                            token.getPushService(), deviceId);
                    }
                } catch (Exception e) {
                    logger.error("Failed to send push notification to device: {}", deviceId, e);
                }
            }
        } catch (Exception e) {
            logger.error("Error sending logout notification to device: {}", deviceId, e);
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
