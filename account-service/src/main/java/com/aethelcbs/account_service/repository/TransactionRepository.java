package com.aethelcbs.account_service.repository;

import com.aethelcbs.account_service.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.account.id = :accountId AND t.transactionType = 'CREDIT' " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate")
    Long sumCreditsByAccountAndDateRange(@Param("accountId") UUID accountId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.account.id = :accountId AND t.transactionType = 'DEBIT' " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate")
    Long sumDebitsByAccountAndDateRange(@Param("accountId") UUID accountId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}
