package com.aethelcbs.notification_service.service;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aethelcbs.notification_service.dto.PushNotificationRequest;
import com.aethelcbs.notification_service.dto.PushNotificationResult;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * APNS Service using token-based authentication (.p8 key file)
 * Compatible with Pushy 0.15.3
 */
@Service
public class ApnsService implements PushNotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(ApnsService.class);

    @Value("${app.push.apns.enabled:false}")
    private boolean apnsEnabled;

    @Value("${app.push.apns.p8-key-path:#{null}}")
    private String p8KeyPath;

    @Value("${app.push.apns.team-id:#{null}}")
    private String teamId;

    @Value("${app.push.apns.key-id:#{null}}")
    private String keyId;

    @Value("${app.push.apns.bundle-id:#{null}}")
    private String bundleId;

    @Value("${app.push.apns.production:false}")
    private boolean production;

    @Value("${app.push.apns.connection-timeout-seconds:10}")
    private int connectionTimeoutSeconds;

    private ApnsClient apnsClient;

    @PostConstruct
    public void initialize() {
        if (!apnsEnabled) {
            logger.warn("APNS is disabled. Push notifications will not be sent.");
            return;
        }

        try {
            if (p8KeyPath == null || teamId == null || keyId == null || bundleId == null) {
                logger.warn("APNS configuration incomplete. APNS will not be initialized.");
                return;
            }

            ApnsSigningKey signingKey = ApnsSigningKey.loadFromPkcs8File(
                    new File(p8KeyPath),
                    teamId,
                    keyId
            );

            apnsClient = new ApnsClientBuilder()
                    .setApnsServer(production
                            ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                            : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setSigningKey(signingKey)
                    .setConnectionTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                    .build();

            logger.info("APNS initialized with token-based auth ({} environment)",
                    production ? "PRODUCTION" : "DEVELOPMENT");

        } catch (Exception e) {
            logger.error("Failed to initialize APNS", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (apnsClient != null) {
            try {
                apnsClient.close().join();
                logger.info("APNS client closed");
            } catch (Exception e) {
                logger.error("Error closing APNS client", e);
            }
        }
    }

    @Override
    public PushNotificationResult sendNotification(PushNotificationRequest request) {
        if (!apnsEnabled || apnsClient == null) {
            logger.debug("APNS not enabled or not initialized. Skipping notification.");
            return PushNotificationResult.failure(request.getDeviceToken(), "APNS not enabled");
        }

        try {
            String payload = new SimpleApnsPayloadBuilder()
                    .setAlertTitle(request.getTitle())
                    .setAlertBody(request.getBody())
                    .setSound("default")
                    .addCustomProperty("type",
                            request.getNotificationType() != null
                                    ? request.getNotificationType()
                                    : "notification")
                    .build();

            SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(
                            TokenUtil.sanitizeTokenString(request.getDeviceToken()),
                            bundleId,
                            payload
                    );

            CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>> future =
                    apnsClient.sendNotification(pushNotification);

            PushNotificationResponse<SimpleApnsPushNotification> response =
                    future.get(30, TimeUnit.SECONDS);

            if (response.isAccepted()) {
                logger.info("APNS sent to {}", maskToken(request.getDeviceToken()));
                return PushNotificationResult.success(request.getDeviceToken());
            } else {
                String rejectionReason = response.getRejectionReason().orElse("Unknown reason");
                logger.warn("APNS rejected {} → {}", maskToken(request.getDeviceToken()), rejectionReason);

                PushNotificationResult result =
                        PushNotificationResult.failure(request.getDeviceToken(), rejectionReason);

                response.getTokenInvalidationTimestamp().ifPresent(ts -> {
                    logger.warn("Token invalidated at {}", ts);
                    result.setTokenInvalid(true);
                });

                return result;
            }

        } catch (Exception e) {
            logger.error("APNS send failed {}", maskToken(request.getDeviceToken()), e);
            return PushNotificationResult.failure(request.getDeviceToken(), e.getMessage());
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    @Override
    public boolean isEnabled() {
        return apnsEnabled && apnsClient != null;
    }

    @Override
    public String getSupportedPlatform() {
        return "IOS";
    }

    @Override
    public String getServiceName() {
        return "APNS";
    }
}