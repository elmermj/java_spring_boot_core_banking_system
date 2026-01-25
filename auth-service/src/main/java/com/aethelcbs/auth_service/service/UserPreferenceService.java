package com.aethelcbs.auth_service.service;

import com.aethelcbs.auth_service.entity.User;
import com.aethelcbs.auth_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserPreferenceService {
    
    private final UserRepository userRepository;
    
    public UserPreferenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Transactional
    public void updateBalanceVisibility(UUID accountId, Boolean isBalanceHidden) {
        User user = userRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsBalanceHidden(isBalanceHidden);
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public Boolean getBalanceVisibility(UUID accountId) {
        User user = userRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return user.getIsBalanceHidden();
    }
}
