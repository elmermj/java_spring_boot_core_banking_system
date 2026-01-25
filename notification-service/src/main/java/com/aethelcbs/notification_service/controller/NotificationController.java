package com.aethelcbs.notification_service.controller;

import com.aethelcbs.notification_service.dto.RegisterPushTokenRequest;
import com.aethelcbs.notification_service.dto.SendLogoutNotificationRequest;
import com.aethelcbs.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Register or update a device push token (idempotent)
     * Called internally by auth-service after successful login
     */
    @PostMapping("/register-token")
    public ResponseEntity<?> registerPushToken(@Valid @RequestBody RegisterPushTokenRequest request) {
        try {
            notificationService.registerPushToken(
                request.getUserId(),
                request.getDeviceId(),
                request.getPushToken(),
                request.getPlatform()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Push token registered successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Send logout notifications to devices
     * Called internally by auth-service when old sessions are terminated
     */
    @PostMapping("/send-logout")
    public ResponseEntity<?> sendLogoutNotifications(@Valid @RequestBody SendLogoutNotificationRequest request) {
        try {
            notificationService.sendLogoutNotifications(
                request.getDeviceIds(),
                request.getAccountNumber()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout notifications sent");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
