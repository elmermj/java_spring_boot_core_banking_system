package com.aethelcbs.auth_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aethelcbs.auth_service.client.NotificationServiceClient;
import com.aethelcbs.auth_service.entity.DeviceSession;
import com.aethelcbs.auth_service.entity.OtpRequest;
import com.aethelcbs.auth_service.entity.User;
import com.aethelcbs.auth_service.repository.DeviceSessionRepository;
import com.aethelcbs.auth_service.repository.OtpRequestRepository;
import com.aethelcbs.auth_service.repository.UserRepository;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final OtpRequestRepository otpRequestRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final NotificationServiceClient notificationServiceClient;
    
    @Value("${spring.security.jwt.expiration}")
    private Long jwtExpiration;
    
    @Value("${app.otp.expiration-minutes}")
    private int otpExpirationMinutes;
    
    public AuthService(UserRepository userRepository,
                      DeviceSessionRepository deviceSessionRepository,
                      OtpRequestRepository otpRequestRepository,
                      PasswordService passwordService,
                      JwtService jwtService,
                      OtpService otpService,
                      NotificationServiceClient notificationServiceClient) {
        this.userRepository = userRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.otpRequestRepository = otpRequestRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.notificationServiceClient = notificationServiceClient;
    }
    
    @Transactional
    public LoginResult login(String accountNumber, String password, String deviceId, 
                            String pushToken, String platform, Boolean isDuplicateAllowed, String deviceType) {
        // Validate push token is not null or empty
        if (pushToken == null || pushToken.trim().isEmpty()) {
            throw new RuntimeException("Push token is required for login");
        }
        
        if (platform == null || platform.trim().isEmpty()) {
            throw new RuntimeException("Platform is required for login");
        }
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid account number or password");
        }
        
        User user = userOpt.get();
        
        // Verify password
        if (!passwordService.verifyPassword(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid account number or password");
        }
        
        // Check if duplicate login is allowed
        if (!isDuplicateAllowed && !user.getIsDuplicateAllowed()) {
            // Check for existing active sessions
            List<DeviceSession> activeSessions = deviceSessionRepository.findByUserAndIsActiveTrue(user);
            if (!activeSessions.isEmpty()) {
                // Generate OTP and send to mobile device
                String otp = otpService.generateAndSendOtp(user, deviceId, "REVERIFY");
                return new LoginResult(null, user.getId(), user.getAccountNumber(), true, 
                    "OTP sent to your registered mobile device. Please verify to continue.");
            }
        }
        
        // If duplicate allowed or no active sessions, proceed with login
        // Deactivate old sessions if duplicate not allowed
        if (!isDuplicateAllowed && !user.getIsDuplicateAllowed()) {
            // Deactivate all sessions of the same device type
            List<DeviceSession> existingSessions = deviceSessionRepository.findByUserAndIsActiveTrue(user);
            existingSessions.stream()
                .filter(s -> deviceType.equals(s.getDeviceType()))
                .forEach(s -> {
                    s.setIsActive(false);
                    deviceSessionRepository.save(s);
                });
        }
        
        // Create new session
        String token = jwtService.generateToken(user.getId(), user.getAccountNumber());
        String tokenHash = passwordService.hashOtp(token); // Using hashOtp for token hashing
        
        DeviceSession session = new DeviceSession();
        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setDeviceType(deviceType);
        session.setTokenHash(tokenHash);
        session.setIsActive(true);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpiration / 1000));
        deviceSessionRepository.save(session);
        
        // Auto-register push token with notification-service (idempotent)
        try {
            notificationServiceClient.registerPushToken(user.getId(), deviceId, pushToken, platform);
        } catch (Exception e) {
            // Log but don't fail login if push token registration fails
            logger.error("Failed to register push token during login", e);
        }
        
        return new LoginResult(token, user.getId(), user.getAccountNumber(), false, null);
    }
    
    @Transactional
    public LoginResult reverify(String accountNumber, String deviceId, String otp) {
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid account number");
        }
        
        User user = userOpt.get();
        
        // Verify OTP
        Optional<OtpRequest> otpRequestOpt = otpRequestRepository.findByUserAndDeviceIdAndPurposeAndIsUsedFalseAndExpiresAtAfter(
            user, deviceId, "REVERIFY", LocalDateTime.now());
        
        if (otpRequestOpt.isEmpty()) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        OtpRequest otpRequest = otpRequestOpt.get();
        if (!passwordService.verifyOtp(otp, otpRequest.getOtpHash())) {
            throw new RuntimeException("Invalid OTP");
        }
        
        // Mark OTP as used
        otpRequest.setIsUsed(true);
        otpRequestRepository.save(otpRequest);
        
        // Only after OTP verification, deactivate old mobile sessions (excluding the new device)
        List<DeviceSession> existingSessions = deviceSessionRepository.findByUserAndIsActiveTrue(user);
        List<DeviceSession> oldMobileSessions = existingSessions.stream()
            .filter(s -> "MOBILE".equals(s.getDeviceType()) && !deviceId.equals(s.getDeviceId()))
            .toList();
        
        // Deactivate old sessions and collect device IDs for notifications
        List<String> oldDeviceIds = oldMobileSessions.stream()
            .map(DeviceSession::getDeviceId)
            .toList();
        
        oldMobileSessions.forEach(s -> {
            s.setIsActive(false);
            deviceSessionRepository.save(s);
        });
        
        // Send logout notifications to old devices
        if (!oldDeviceIds.isEmpty()) {
            notificationServiceClient.sendLogoutNotifications(oldDeviceIds, user.getAccountNumber());
        }
        
        // Create new session for the new device
        String token = jwtService.generateToken(user.getId(), user.getAccountNumber());
        String tokenHash = passwordService.hashOtp(token);
        
        DeviceSession session = new DeviceSession();
        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setDeviceType("MOBILE");
        session.setTokenHash(tokenHash);
        session.setIsActive(true);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpiration / 1000));
        deviceSessionRepository.save(session);
        
        return new LoginResult(token, user.getId(), user.getAccountNumber(), false, null);
    }
    
    @Transactional
    public LoginResult webLogin(String accountNumber, String password, String deviceId, String otp) {
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid account number or password");
        }
        
        User user = userOpt.get();
        
        // Verify password
        if (!passwordService.verifyPassword(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid account number or password");
        }
        
        // Verify OTP
        Optional<OtpRequest> otpRequestOpt = otpRequestRepository.findByUserAndDeviceIdAndPurposeAndIsUsedFalseAndExpiresAtAfter(
            user, deviceId, "WEB_LOGIN", LocalDateTime.now());
        
        if (otpRequestOpt.isEmpty()) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        OtpRequest otpRequest = otpRequestOpt.get();
        if (!passwordService.verifyOtp(otp, otpRequest.getOtpHash())) {
            throw new RuntimeException("Invalid OTP");
        }
        
        // Mark OTP as used
        otpRequest.setIsUsed(true);
        otpRequestRepository.save(otpRequest);
        
        // Create new session (web login allows multiple sessions)
        String token = jwtService.generateToken(user.getId(), user.getAccountNumber());
        String tokenHash = passwordService.hashOtp(token);
        
        DeviceSession session = new DeviceSession();
        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setDeviceType("WEB");
        session.setTokenHash(tokenHash);
        session.setIsActive(true);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpiration / 1000));
        deviceSessionRepository.save(session);
        
        return new LoginResult(token, user.getId(), user.getAccountNumber(), false, null);
    }
    
    public void requestWebLoginOtp(String accountNumber, String deviceId) {
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid account number");
        }
        
        User user = userOpt.get();
        
        // Find active mobile session to send OTP
        List<DeviceSession> mobileSessions = deviceSessionRepository.findByUserAndIsActiveTrue(user);
        boolean hasMobileSession = mobileSessions.stream()
            .anyMatch(s -> "MOBILE".equals(s.getDeviceType()));
        
        if (!hasMobileSession) {
            throw new RuntimeException("No active mobile session found. Please login to mobile app first.");
        }
        
        // Generate and send OTP
        otpService.generateAndSendOtp(user, deviceId, "WEB_LOGIN");
    }
    
    public static class LoginResult {
        private final String token;
        private final UUID accountId;
        private final String accountNumber;
        private final Boolean requiresOtp;
        private final String message;
        
        public LoginResult(String token, UUID accountId, String accountNumber, 
                          Boolean requiresOtp, String message) {
            this.token = token;
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.requiresOtp = requiresOtp;
            this.message = message;
        }
        
        public String getToken() { return token; }
        public UUID getAccountId() { return accountId; }
        public String getAccountNumber() { return accountNumber; }
        public Boolean getRequiresOtp() { return requiresOtp; }
        public String getMessage() { return message; }
    }
}
