package com.aethelcbs.account_service.repository;

import com.aethelcbs.account_service.entity.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, UUID> {
    Optional<MonthlySummary> findByAccountIdAndYearAndMonth(UUID accountId, Integer year, Integer month);
}
