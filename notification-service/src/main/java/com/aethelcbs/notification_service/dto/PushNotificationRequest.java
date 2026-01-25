package com.aethelcbs.notification_service.dto;

import jakarta.validation.constraints.NotBlank;

public class PushNotificationRequest {
    @NotBlank(message = "Device token is required")
    private String deviceToken;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private String platform; // ANDROID, IOS
    
    private String pushService; // FCM, APNS
    
    private String notificationType; // logout, transaction, etc.
    
    // Getters and Setters
    public String getDeviceToken() {
        return deviceToken;
    }
    
    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public String getPushService() {
        return pushService;
    }
    
    public void setPushService(String pushService) {
        this.pushService = pushService;
    }
    
    public String getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
}
