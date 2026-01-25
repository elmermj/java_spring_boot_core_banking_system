package com.aethelcbs.auth_service.controller;

import com.aethelcbs.auth_service.dto.*;
import com.aethelcbs.auth_service.service.AuthService;
import com.aethelcbs.auth_service.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    private final UserPreferenceService userPreferenceService;
    
    public AuthController(AuthService authService, 
                         UserPreferenceService userPreferenceService) {
        this.authService = authService;
        this.userPreferenceService = userPreferenceService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.LoginResult result = authService.login(
                request.getAccountNumber(),
                request.getPassword(),
                request.getDeviceId(),
                request.getPushToken(),
                request.getPlatform(),
                request.getIsDuplicateAllowed(),
                request.getDeviceType()
            );
            
            if (result.getRequiresOtp()) {
                LoginResponse response = new LoginResponse();
                response.setAccountId(result.getAccountId());
                response.setAccountNumber(result.getAccountNumber());
                response.setRequiresOtp(true);
                response.setMessage(result.getMessage());
                return ResponseEntity.ok(response);
            } else {
                LoginResponse response = new LoginResponse(
                    result.getToken(),
                    result.getAccountId(),
                    result.getAccountNumber()
                );
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @PostMapping("/reverify")
    public ResponseEntity<?> reverify(@Valid @RequestBody ReverifyRequest request) {
        try {
            AuthService.LoginResult result = authService.reverify(
                request.getAccountNumber(),
                request.getDeviceId(),
                request.getOtp()
            );
            
            LoginResponse response = new LoginResponse(
                result.getToken(),
                result.getAccountId(),
                result.getAccountNumber()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @PostMapping("/web-login/request-otp")
    public ResponseEntity<?> requestWebLoginOtp(@RequestBody Map<String, String> request) {
        try {
            String accountNumber = request.get("accountNumber");
            String deviceId = request.get("deviceId");
            
            if (accountNumber == null || deviceId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Account number and device ID are required");
                return ResponseEntity.badRequest().body(error);
            }
            
            authService.requestWebLoginOtp(accountNumber, deviceId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP sent to your registered mobile device");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/web-login")
    public ResponseEntity<?> webLogin(@Valid @RequestBody WebLoginRequest request) {
        try {
            AuthService.LoginResult result = authService.webLogin(
                request.getAccountNumber(),
                request.getPassword(),
                request.getDeviceId(),
                request.getOtp()
            );
            
            LoginResponse response = new LoginResponse(
                result.getToken(),
                result.getAccountId(),
                result.getAccountNumber()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @PutMapping("/balance-visibility")
    public ResponseEntity<?> updateBalanceVisibility(@RequestHeader("X-Account-Id") String accountIdHeader,
                                                      @RequestBody Map<String, Boolean> request) {
        try {
            UUID accountId = UUID.fromString(accountIdHeader);
            Boolean isBalanceHidden = request.get("isBalanceHidden");
            
            if (isBalanceHidden == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "isBalanceHidden parameter is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            userPreferenceService.updateBalanceVisibility(accountId, isBalanceHidden);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Balance visibility updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid account ID format");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
