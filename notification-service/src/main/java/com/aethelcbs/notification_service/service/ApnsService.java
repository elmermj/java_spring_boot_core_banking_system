package com.aethelcbs.notification_service.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Service
public class ApnsService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApnsService.class);
    
    @Value("${app.push.apns.enabled:false}")
    private boolean apnsEnabled;
    
    @Value("${app.push.apns.key-store-path:#{null}}")
    private String keyStorePath;
    
    @Value("${app.push.apns.key-store-password:#{null}}")
    private String keyStorePassword;
    
    @Value("${app.push.apns.team-id:#{null}}")
    private String teamId;
    
    @Value("${app.push.apns.key-id:#{null}}")
    private String keyId;
    
    @Value("${app.push.apns.bundle-id:#{null}}")
    private String bundleId;
    
    @Value("${app.push.apns.production:false}")
    private boolean production;
    
    private ApnsClient apnsClient;
    
    @PostConstruct
    public void initialize() {
        if (!apnsEnabled) {
            logger.warn("APNS is disabled. Push notifications will not be sent.");
            return;
        }
        
        try {
            if (keyStorePath == null || keyStorePath.isEmpty() || 
                keyStorePassword == null || keyStorePassword.isEmpty() ||
                teamId == null || keyId == null || bundleId == null) {
                logger.warn("APNS configuration incomplete. APNS will not be initialized.");
                return;
            }
            
            File keyStoreFile = new File(keyStorePath);
            if (!keyStoreFile.exists()) {
                logger.error("APNS key store file not found: {}", keyStorePath);
                return;
            }
            
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream inputStream = new FileInputStream(keyStoreFile)) {
                keyStore.load(inputStream, keyStorePassword.toCharArray());
            }
            
            ApnsClientBuilder builder = new ApnsClientBuilder()
                .setApnsServer(production ? 
                    ApnsClientBuilder.PRODUCTION_APNS_HOST : 
                    ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setClientCredentials(keyStoreFile, keyStorePassword);
            
            apnsClient = builder.build();
            logger.info("APNS initialized successfully ({} environment)", 
                production ? "PRODUCTION" : "DEVELOPMENT");
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            logger.error("Failed to initialize APNS", e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (apnsClient != null) {
            Future<Void> closeFuture = apnsClient.close();
            try {
                closeFuture.await();
                logger.info("APNS client closed");
            } catch (InterruptedException e) {
                logger.error("Error closing APNS client", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void sendNotification(String deviceToken, String title, String body) {
        if (!apnsEnabled || apnsClient == null) {
            logger.debug("APNS not enabled or not initialized. Skipping notification.");
            return;
        }
        
        try {
            String payload = new ApnsPayloadBuilder()
                .setAlertTitle(title)
                .setAlertBody(body)
                .setSound("default")
                .addCustomProperty("type", "logout")
                .buildWithDefaultMaximumLength();
            
            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(deviceToken),
                bundleId,
                payload
            );
            
            Future<PushNotificationResponse<SimpleApnsPushNotification>> future = 
                apnsClient.sendNotification(pushNotification);
            
            future.addListener((Future<PushNotificationResponse<SimpleApnsPushNotification>> f) -> {
                if (f.isSuccess()) {
                    PushNotificationResponse<SimpleApnsPushNotification> response = f.getNow();
                    if (response.isAccepted()) {
                        logger.info("Successfully sent APNS notification");
                    } else {
                        logger.warn("APNS notification rejected: {}", response.getRejectionReason());
                        if (response.getTokenInvalidationTimestamp() != null) {
                            logger.warn("Token invalidated at: {}", response.getTokenInvalidationTimestamp());
                        }
                    }
                } else {
                    logger.error("Failed to send APNS notification", f.cause());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to send APNS notification to token: {}", deviceToken, e);
        }
    }
    
    public boolean isEnabled() {
        return apnsEnabled && apnsClient != null;
    }
}
