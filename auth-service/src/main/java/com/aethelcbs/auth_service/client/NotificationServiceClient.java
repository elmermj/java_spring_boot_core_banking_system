package com.aethelcbs.auth_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceClient.class);
    
    @Value("${app.notification-service.url}")
    private String notificationServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public NotificationServiceClient() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Register push token (idempotent)
     */
    public void registerPushToken(UUID userId, String deviceId, String pushToken, String platform) {
        try {
            String url = notificationServiceUrl + "/api/notifications/register-token";
            
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("deviceId", deviceId);
            request.put("pushToken", pushToken);
            request.put("platform", platform);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            logger.info("Push token registered successfully for device: {}", deviceId);
        } catch (Exception e) {
            logger.error("Failed to register push token for device: {}", deviceId, e);
            // Don't throw exception - push token registration failure shouldn't block login
        }
    }
    
    /**
     * Send logout notifications to devices
     */
    public void sendLogoutNotifications(List<String> deviceIds, String accountNumber) {
        try {
            String url = notificationServiceUrl + "/api/notifications/send-logout";
            
            Map<String, Object> request = new HashMap<>();
            request.put("deviceIds", deviceIds);
            request.put("accountNumber", accountNumber);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            logger.info("Logout notifications sent successfully to {} devices", deviceIds.size());
        } catch (Exception e) {
            logger.error("Failed to send logout notifications", e);
            // Don't throw exception - notification failure shouldn't block authentication
        }
    }
}
