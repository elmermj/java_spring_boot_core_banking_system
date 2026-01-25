package com.aethelcbs.auth_service.repository;

import com.aethelcbs.auth_service.entity.OtpRequest;
import com.aethelcbs.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, UUID> {
    Optional<OtpRequest> findByUserAndDeviceIdAndPurposeAndIsUsedFalseAndExpiresAtAfter(
        User user, String deviceId, String purpose, LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM OtpRequest o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
}
