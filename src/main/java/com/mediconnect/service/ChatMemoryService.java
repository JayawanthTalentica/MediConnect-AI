package com.mediconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryService.class);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, List<String>> inMemoryHistory = new ConcurrentHashMap<>();

    public ChatMemoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void appendMessage(String sessionId, String role, String content) {
        String entry = role + ": " + content;
        String redisKey = "chat:history:" + sessionId;

        try {
            redisTemplate.opsForList().rightPush(redisKey, entry);
            redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
            log.info("Appended message to Redis session '{}': [{}]", sessionId, role);
        } catch (Exception e) {
            log.warn("Redis unavailable. Storing conversation turn in local memory fallback.");
            inMemoryHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(entry);
        }
    }

    public List<String> getHistory(String sessionId) {
        String redisKey = "chat:history:" + sessionId;
        try {
            List<String> history = redisTemplate.opsForList().range(redisKey, 0, -1);
            if (history != null && !history.isEmpty()) {
                return history;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable. Fetching conversation turn from local memory fallback.");
        }
        return inMemoryHistory.getOrDefault(sessionId, new ArrayList<>());
    }

    public void clearHistory(String sessionId) {
        String redisKey = "chat:history:" + sessionId;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception ignored) {}
        inMemoryHistory.remove(sessionId);
    }
}
