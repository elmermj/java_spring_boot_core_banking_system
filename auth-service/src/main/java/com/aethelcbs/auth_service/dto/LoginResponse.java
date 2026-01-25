package com.aethelcbs.auth_service.dto;

import java.util.UUID;

public class LoginResponse {
    private String token;
    private UUID accountId;
    private String accountNumber;
    private Boolean requiresOtp;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(String token, UUID accountId, String accountNumber) {
        this.token = token;
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.requiresOtp = false;
    }

    public LoginResponse(UUID accountId, String accountNumber, String message) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.requiresOtp = true;
        this.message = message;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Boolean getRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(Boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
