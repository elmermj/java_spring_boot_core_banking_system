package com.aethelcbs.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class RegisterPushTokenRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Push token is required")
    private String pushToken;

    @NotBlank(message = "Platform is required")
    private String platform; // ANDROID, IOS

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
