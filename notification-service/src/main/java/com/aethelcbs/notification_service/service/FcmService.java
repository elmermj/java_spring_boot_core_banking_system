package com.aethelcbs.notification_service.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FcmService {
    
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
    
    public void sendNotification(String fcmToken, String title, String body) {
        if (!fcmEnabled || firebaseMessaging == null) {
            logger.debug("FCM not enabled or not initialized. Skipping notification.");
            return;
        }
        
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("type", "logout")
                .build();
            
            String response = firebaseMessaging.send(message);
            logger.info("Successfully sent FCM message: {}", response);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM notification to token: {}", fcmToken, e);
            // Handle invalid tokens - could mark token as inactive
            if (e.getErrorCode() != null && 
                (e.getErrorCode().equals("invalid-registration-token") || 
                 e.getErrorCode().equals("registration-token-not-registered"))) {
                logger.warn("Invalid FCM token detected: {}", fcmToken);
            }
        }
    }
    
    public boolean isEnabled() {
        return fcmEnabled && firebaseMessaging != null;
    }
}
