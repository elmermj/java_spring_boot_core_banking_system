package com.aethelcbs.notification_service.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class SendLogoutNotificationRequest {
    @NotEmpty(message = "Device IDs are required")
    private List<String> deviceIds;

    private String accountNumber;

    // Getters and Setters
    public List<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
