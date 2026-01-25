package com.aethelcbs.account_service.service;

import com.aethelcbs.account_service.dto.HomeResponse;
import com.aethelcbs.account_service.entity.Account;
import com.aethelcbs.account_service.entity.MonthlySummary;
import com.aethelcbs.account_service.entity.Transaction;
import com.aethelcbs.account_service.repository.AccountRepository;
import com.aethelcbs.account_service.repository.MonthlySummaryRepository;
import com.aethelcbs.account_service.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final MonthlySummaryRepository monthlySummaryRepository;
    
    private static final String BALANCE_MASKED = "****";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public AccountService(AccountRepository accountRepository,
                         TransactionRepository transactionRepository,
                         MonthlySummaryRepository monthlySummaryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.monthlySummaryRepository = monthlySummaryRepository;
    }
    
    @Transactional(readOnly = true)
    public HomeResponse getHomeData(UUID accountId, Boolean isBalanceHidden) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        HomeResponse response = new HomeResponse();
        response.setAccountId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setCurrency(account.getCurrency());
        
        // Set balance (masked if hidden)
        if (isBalanceHidden != null && isBalanceHidden) {
            response.setBalance(BALANCE_MASKED);
        } else {
            response.setBalance(formatAmount(account.getBalance(), account.getCurrency()));
        }
        
        // Get latest 5 transactions
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(
            accountId, PageRequest.of(0, 5));
        
        response.setLatestTransactions(transactions.stream()
            .map(t -> {
                HomeResponse.TransactionDto dto = new HomeResponse.TransactionDto();
                dto.setId(t.getId());
                dto.setReferenceId(t.getReferenceId());
                dto.setType(t.getTransactionType());
                dto.setAmount(formatAmount(t.getAmount(), account.getCurrency()));
                dto.setDescription(t.getDescription());
                dto.setCreatedAt(t.getCreatedAt().format(DATE_FORMATTER));
                return dto;
            })
            .collect(Collectors.toList()));
        
        // Get monthly stats
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        
        Optional<MonthlySummary> summaryOpt = monthlySummaryRepository.findByAccountIdAndYearAndMonth(
            accountId, year, month);
        
        HomeResponse.MonthlyStatsDto monthlyStats = new HomeResponse.MonthlyStatsDto();
        monthlyStats.setCurrency(account.getCurrency());
        
        if (summaryOpt.isPresent()) {
            MonthlySummary summary = summaryOpt.get();
            monthlyStats.setTotalIncome(formatAmount(summary.getTotalIncome(), account.getCurrency()));
            monthlyStats.setTotalExpenditure(formatAmount(summary.getTotalExpenditure(), account.getCurrency()));
        } else {
            // Calculate on the fly if not cached
            LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
            LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
            
            Long totalIncome = transactionRepository.sumCreditsByAccountAndDateRange(
                accountId, startOfMonth, startOfNextMonth);
            Long totalExpenditure = transactionRepository.sumDebitsByAccountAndDateRange(
                accountId, startOfMonth, startOfNextMonth);
            
            monthlyStats.setTotalIncome(formatAmount(totalIncome != null ? totalIncome : 0L, account.getCurrency()));
            monthlyStats.setTotalExpenditure(formatAmount(totalExpenditure != null ? totalExpenditure : 0L, account.getCurrency()));
        }
        
        response.setMonthlyStats(monthlyStats);
        
        return response;
    }
    
    @Transactional(readOnly = true)
    public HomeResponse toggleBalanceVisibility(UUID accountId, Boolean balanceVisible) {
        return getHomeData(accountId, !balanceVisible);
    }
    
    private String formatAmount(Long amount, String currency) {
        // Convert from smallest currency unit (cents) to major unit
        BigDecimal majorUnit = BigDecimal.valueOf(amount)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return majorUnit.toPlainString() + " " + currency;
    }
}
