package com.aethelcbs.notification_service.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aethelcbs.notification_service.dto.PushNotificationRequest;
import com.aethelcbs.notification_service.dto.PushNotificationResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import jakarta.annotation.PostConstruct;

@Service
public class FcmService implements PushNotificationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(FcmService.class);
    
    @Value("${app.push.fcm.enabled:false}")
    private boolean fcmEnabled;
    
    @Value("${app.push.fcm.service-account-key:#{null}}")
    private String serviceAccountKeyPath;
    
    private FirebaseMessaging firebaseMessaging;
    
    @PostConstruct
    public void initialize() {
        if (!fcmEnabled) {
            logger.warn("FCM is disabled. Push notifications will not be sent.");
            return;
        }
        
        try {
            if (serviceAccountKeyPath == null || serviceAccountKeyPath.isEmpty()) {
                logger.warn("FCM service account key path not configured. FCM will not be initialized.");
                return;
            }
            
            InputStream serviceAccount = new FileInputStream(serviceAccountKeyPath);
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
                .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            firebaseMessaging = FirebaseMessaging.getInstance();
            logger.info("FCM initialized successfully");
        } catch (IOException e) {
            logger.error("Failed to initialize FCM", e);
        }
    }
    
    @Override
    public PushNotificationResult sendNotification(PushNotificationRequest request) {
        if (!fcmEnabled || firebaseMessaging == null) {
            logger.debug("FCM not enabled or not initialized. Skipping notification.");
            return PushNotificationResult.failure(request.getDeviceToken(), "FCM not enabled");
        }
        
        try {
            Message message = Message.builder()
                .setToken(request.getDeviceToken())
                .setNotification(Notification.builder()
                    .setTitle(request.getTitle())
                    .setBody(request.getBody())
                    .build())
                .putData("type", request.getNotificationType() != null ? 
                    request.getNotificationType() : "notification")
                .build();
            
            String response = firebaseMessaging.send(message);
            logger.info("Successfully sent FCM message: {} to token: {}", 
                response, maskToken(request.getDeviceToken()));
            return PushNotificationResult.success(request.getDeviceToken());
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM notification to token: {}", 
                maskToken(request.getDeviceToken()), e);
            
            PushNotificationResult result = PushNotificationResult.failure(
                request.getDeviceToken(), e.getMessage());
            
            // Check for invalid token errors
            if (e.getErrorCode() != null) {
                String errorCode = e.getErrorCode().toString();
                if (errorCode.equals("invalid-registration-token") || 
                    errorCode.equals("registration-token-not-registered") ||
                    errorCode.equals("UNREGISTERED")) {
                    result.setTokenInvalid(true);
                    logger.warn("Invalid FCM token detected: {}", maskToken(request.getDeviceToken()));
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Unexpected error sending FCM notification", e);
            return PushNotificationResult.failure(request.getDeviceToken(), 
                "Unexpected error: " + e.getMessage());
        }
    }
    
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
    
    @Override
    public boolean isEnabled() {
        return fcmEnabled && firebaseMessaging != null;
    }
    
    @Override
    public String getSupportedPlatform() {
        return "ANDROID";
    }
    
    @Override
    public String getServiceName() {
        return "FCM";
    }
}
