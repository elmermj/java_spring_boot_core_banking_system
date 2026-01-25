package com.aethelcbs.api_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${app.rate-limit.requests-per-minute:20}")
    private int requestsPerMinute;
    
    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check if request is within rate limit
     * @param key - typically accountId or IP address
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key) {
        if (!rateLimitEnabled) {
            return true;
        }
        
        try {
            String redisKey = "rate_limit:" + key;
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            
            String currentCount = ops.get(redisKey);
            if (currentCount == null) {
                // First request in the window
                ops.set(redisKey, "1", 60, TimeUnit.SECONDS);
                return true;
            }
            
            int count = Integer.parseInt(currentCount);
            if (count >= requestsPerMinute) {
                logger.warn("Rate limit exceeded for key: {}", key);
                return false;
            }
            
            // Increment counter
            ops.increment(redisKey);
            return true;
        } catch (Exception e) {
            logger.error("Error checking rate limit for key: {}", key, e);
            // Fail open - allow request if rate limit check fails
            return true;
        }
    }
    
    /**
     * Get remaining requests in current window
     */
    public int getRemainingRequests(String key) {
        try {
            String redisKey = "rate_limit:" + key;
            String currentCount = redisTemplate.opsForValue().get(redisKey);
            if (currentCount == null) {
                return requestsPerMinute;
            }
            int count = Integer.parseInt(currentCount);
            return Math.max(0, requestsPerMinute - count);
        } catch (Exception e) {
            logger.error("Error getting remaining requests for key: {}", key, e);
            return requestsPerMinute;
        }
    }
}
