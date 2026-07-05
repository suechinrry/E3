package com.visitor.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AI 话术缓存服务 —— Redis 主 + 内存降级。
 *
 * 缓存键 = SHA-256（访客姓名 + 单位 + 事由 + 被访人），TTL = 12 小时。
 * Redis 未连接时自动降级为 ConcurrentHashMap + 手动过期淘汰。
 */
@Slf4j
@Service
public class GreetingCacheService {

    private static final String KEY_PREFIX = "greeting:cache:";
    private static final long TTL_HOURS = 12;

    private final StringRedisTemplate redisTemplate;
    private final boolean redisAvailable;

    private final ConcurrentHashMap<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final String json;
        final long expireAt;
        CacheEntry(String json) {
            this.json = json;
            this.expireAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TTL_HOURS);
        }
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    public GreetingCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        boolean available;
        try {
            redisTemplate.opsForValue().get("__health_check__");
            available = true;
            log.info("Redis 连接正常，话术缓存使用 Redis 存储");
        } catch (Exception e) {
            available = false;
            log.warn("Redis 不可用（{}），降级为内存缓存", e.getMessage());
        }
        this.redisAvailable = available;
    }

    public String buildCacheKey(String visitorName, String company, String purpose, String hostName) {
        String raw = norm(visitorName) + "|" + norm(company) + "|" + norm(purpose) + "|" + norm(hostName);
        return KEY_PREFIX + sha256(raw);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> get(String cacheKey) {
        try {
            if (redisAvailable) {
                String json = redisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
                }
            } else {
                CacheEntry entry = memoryCache.get(cacheKey);
                if (entry != null) {
                    if (entry.isExpired()) { memoryCache.remove(cacheKey); return null; }
                    return new com.fasterxml.jackson.databind.ObjectMapper().readValue(entry.json, Map.class);
                }
            }
        } catch (Exception e) {
            log.warn("缓存读取异常: key={}", cacheKey);
            evict(cacheKey);
        }
        return null;
    }

    public void put(String cacheKey, Map<String, String> greetingMap) {
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(greetingMap);
            if (redisAvailable) {
                redisTemplate.opsForValue().set(cacheKey, json, TTL_HOURS, TimeUnit.HOURS);
            } else {
                memoryCache.put(cacheKey, new CacheEntry(json));
            }
        } catch (Exception e) {
            log.warn("缓存写入失败: key={}", cacheKey);
        }
    }

    public void evict(String cacheKey) {
        try {
            if (redisAvailable) redisTemplate.delete(cacheKey);
            memoryCache.remove(cacheKey);
        } catch (Exception e) {
            log.warn("缓存清除异常: key={}", cacheKey);
        }
    }

    private String norm(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", ""); }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
