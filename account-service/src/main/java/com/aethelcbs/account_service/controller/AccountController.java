package com.aethelcbs.account_service.controller;

import com.aethelcbs.account_service.dto.HomeResponse;
import com.aethelcbs.account_service.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    
    private final AccountService accountService;
    
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    @GetMapping("/home")
    public ResponseEntity<?> getHome(@RequestHeader("X-Account-Id") String accountIdHeader,
                                    @RequestParam(value = "balance-visible", required = false) Boolean balanceVisible) {
        try {
            UUID accountId = UUID.fromString(accountIdHeader);
            Boolean isBalanceHidden = balanceVisible != null ? !balanceVisible : null;
            
            HomeResponse response = accountService.getHomeData(accountId, isBalanceHidden);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid account ID format");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/balance-visibility")
    public ResponseEntity<?> toggleBalanceVisibility(@RequestHeader("X-Account-Id") String accountIdHeader,
                                                     @RequestBody Map<String, Boolean> request) {
        try {
            UUID accountId = UUID.fromString(accountIdHeader);
            Boolean balanceVisible = request.get("balanceVisible");
            
            if (balanceVisible == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "balanceVisible parameter is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            HomeResponse response = accountService.toggleBalanceVisibility(accountId, balanceVisible);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid account ID format");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
