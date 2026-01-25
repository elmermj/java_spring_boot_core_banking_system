package com.aethelcbs.notification_service.dto;

import java.time.LocalDateTime;

public class PushNotificationResult {
    private boolean success;
    private String deviceToken;
    private String errorMessage;
    private String rejectionReason;
    private LocalDateTime timestamp;
    private boolean tokenInvalid;
    private int retryCount;
    
    public PushNotificationResult(boolean success, String deviceToken) {
        this.success = success;
        this.deviceToken = deviceToken;
        this.timestamp = LocalDateTime.now();
        this.retryCount = 0;
    }
    
    public static PushNotificationResult success(String deviceToken) {
        return new PushNotificationResult(true, deviceToken);
    }
    
    public static PushNotificationResult failure(String deviceToken, String errorMessage) {
        PushNotificationResult result = new PushNotificationResult(false, deviceToken);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    public static PushNotificationResult tokenInvalid(String deviceToken) {
        PushNotificationResult result = new PushNotificationResult(false, deviceToken);
        result.setTokenInvalid(true);
        result.setErrorMessage("Device token is invalid or unregistered");
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getDeviceToken() {
        return deviceToken;
    }
    
    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isTokenInvalid() {
        return tokenInvalid;
    }
    
    public void setTokenInvalid(boolean tokenInvalid) {
        this.tokenInvalid = tokenInvalid;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
