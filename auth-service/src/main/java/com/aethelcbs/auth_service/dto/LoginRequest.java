package com.aethelcbs.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LoginRequest {
    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Push token is required")
    private String pushToken;

    @NotBlank(message = "Platform is required")
    private String platform; // ANDROID, IOS

    @NotNull(message = "isDuplicateAllowed is required")
    private Boolean isDuplicateAllowed;

    private String deviceType = "MOBILE"; // MOBILE or WEB

    // Getters and Setters
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Boolean getIsDuplicateAllowed() {
        return isDuplicateAllowed;
    }

    public void setIsDuplicateAllowed(Boolean isDuplicateAllowed) {
        this.isDuplicateAllowed = isDuplicateAllowed;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
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
