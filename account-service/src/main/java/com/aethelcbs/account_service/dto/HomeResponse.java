package com.aethelcbs.account_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HomeResponse {
    private UUID accountId;
    private String accountNumber;
    private String balance; // Will be masked if hidden
    private String currency;
    private List<TransactionDto> latestTransactions;
    private MonthlyStatsDto monthlyStats;

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<TransactionDto> getLatestTransactions() {
        return latestTransactions;
    }

    public void setLatestTransactions(List<TransactionDto> latestTransactions) {
        this.latestTransactions = latestTransactions;
    }

    public MonthlyStatsDto getMonthlyStats() {
        return monthlyStats;
    }

    public void setMonthlyStats(MonthlyStatsDto monthlyStats) {
        this.monthlyStats = monthlyStats;
    }

    public static class TransactionDto {
        private UUID id;
        private String referenceId;
        private String type; // DEBIT, CREDIT
        private String amount;
        private String description;
        private String createdAt;

        // Getters and Setters
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public void setReferenceId(String referenceId) {
            this.referenceId = referenceId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class MonthlyStatsDto {
        private String totalIncome;
        private String totalExpenditure;
        private String currency;

        // Getters and Setters
        public String getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(String totalIncome) {
            this.totalIncome = totalIncome;
        }

        public String getTotalExpenditure() {
            return totalExpenditure;
        }

        public void setTotalExpenditure(String totalExpenditure) {
            this.totalExpenditure = totalExpenditure;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
