package com.aethelcbs.auth_service.repository;

import com.aethelcbs.auth_service.entity.DeviceSession;
import com.aethelcbs.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {
    Optional<DeviceSession> findByUserAndDeviceIdAndDeviceType(User user, String deviceId, String deviceType);
    
    List<DeviceSession> findByUserAndIsActiveTrue(User user);
    
    
    @Modifying
    @Query("DELETE FROM DeviceSession ds WHERE ds.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
}
