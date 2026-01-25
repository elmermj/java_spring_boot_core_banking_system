package com.aethelcbs.notification_service.repository;

import com.aethelcbs.notification_service.entity.DevicePushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, UUID> {
    Optional<DevicePushToken> findByDeviceIdAndUserIdAndPlatform(String deviceId, UUID userId, String platform);
    
    List<DevicePushToken> findByDeviceIdAndIsActiveTrue(String deviceId);
    
    List<DevicePushToken> findByUserIdAndIsActiveTrue(UUID userId);
}
