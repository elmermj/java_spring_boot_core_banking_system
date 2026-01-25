package com.aethelcbs.auth_service.service;

import com.aethelcbs.auth_service.entity.OtpRequest;
import com.aethelcbs.auth_service.entity.User;
import com.aethelcbs.auth_service.repository.OtpRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OtpService {
    
    private final OtpRequestRepository otpRequestRepository;
    private final PasswordService passwordService;
    
    @Value("${app.otp.length}")
    private int otpLength;
    
    @Value("${app.otp.expiration-minutes}")
    private int otpExpirationMinutes;
    
    public OtpService(OtpRequestRepository otpRequestRepository, PasswordService passwordService) {
        this.otpRequestRepository = otpRequestRepository;
        this.passwordService = passwordService;
    }
    
    @Transactional
    public String generateAndSendOtp(User user, String deviceId, String purpose) {
        // Generate OTP
        String otp = passwordService.generateOtp(otpLength);
        String otpHash = passwordService.hashOtp(otp);
        
        // Save OTP request
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setUser(user);
        otpRequest.setDeviceId(deviceId);
        otpRequest.setOtpCode(otp);
        otpRequest.setOtpHash(otpHash);
        otpRequest.setPurpose(purpose);
        otpRequest.setIsUsed(false);
        otpRequest.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        otpRequestRepository.save(otpRequest);
        
        // In a real implementation, send OTP via SMS/push notification
        // For now, we'll log it (in production, use SMS gateway or push notification service)
        System.out.println("OTP for user " + user.getAccountNumber() + " (" + purpose + "): " + otp);
        
        return otp;
    }
}
