package com.aethelcbs.auth_service.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

@Service
public class PasswordService {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String ALGORITHM = "SHA-256";
    
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }
    
    public boolean verifyPassword(String password, String storedHash) {
        return passwordEncoder.matches(password, storedHash);
    }
    
    public String generateOtp(int length) {
        Random random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    public String hashOtp(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = md.digest(otp.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing OTP", e);
        }
    }
    
    public boolean verifyOtp(String otp, String storedHash) {
        String hashedOtp = hashOtp(otp);
        return hashedOtp.equals(storedHash);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
