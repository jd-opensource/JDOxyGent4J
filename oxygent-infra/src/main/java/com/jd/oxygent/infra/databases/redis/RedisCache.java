package com.jd.oxygent.infra.databases.redis;

import com.jd.oxygent.core.oxygent.infra.databases.BaseCache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis cache operations.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "oxygent.cache", havingValue = "redis")
public class RedisCache implements BaseCache {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // ==================== List Operations ====================

    @Override
    public int lpush(String key, String[] valueList) {
        Long num = redisTemplate.opsForList().leftPushAll(key, valueList);
        // 1 day
        int defaultExpireTime = 86400;
        redisTemplate.expire(key, defaultExpireTime, TimeUnit.SECONDS);
        return Math.toIntExact(num);
    }

    @Override
    public void close() {
        log.info("redis close ...");
    }

    @Override
    public String rpop(String key) {
        log.info("from redis get key :{}", key);
        return redisTemplate.opsForList().rightPop(key);
    }

    @Override
    public Object brpop(String key, int timeout) {
        // RedisTemplate's BRPOP is blocking, can be used directly
        log.info("from redis get key :{}", key);
        String result = redisTemplate.opsForList().rightPop(key, timeout, TimeUnit.SECONDS);
        return StringUtils.isEmpty(result) ? null : result; // [key, value]
    }

    @Override
    public Object brpop(String key) {
        return brpop(key, 1);
    }

    // ==================== String Operations ====================

    public String get(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception ex) {
            log.error("get key=" + key + " error", ex);
            throw new RuntimeException(ex);
        }
    }

    public Long lpush(String key, Long expiredTime, String... valueList) {
        Long num = redisTemplate.opsForList().leftPushAll(key, valueList);
        redisTemplate.expire(key, expiredTime, TimeUnit.SECONDS);
        return num;
    }

    public boolean expire(String key, int expiredTime) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        try {
            return redisTemplate.expire(key, expiredTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("execute Redis expire error. key: {}, expiredTime: {}s", key, expiredTime, e);
            return false;
        }
    }

    public boolean set(String key, String value) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception ex) {
            log.error("set key={}, value={}; error", key, value, ex);
            throw new RuntimeException(ex);
        }
    }

    public boolean setEx(String key, String value, Integer expireTime) {
        if (StringUtils.isBlank(key) || expireTime == null) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
            return true;
        } catch (Exception ex) {
            log.error("setEx key={}, value={}; error", key, value, ex);
            throw new RuntimeException(ex);
        }
    }

    // ==================== List Full Read ====================

    public List<String> getAllListData(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // ==================== Distributed Lock ====================

    public boolean tryGetDistributedLock(String key, String value, int expireTime) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        try {
            // SET key value NX PX expireTime
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception ex) {
            log.error("tryGetDistributedLock key={} error", key, ex);
            throw new RuntimeException(ex);
        }
    }

    public boolean releaseDistributedLock(String key, String value) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        try {
            String luaScript =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "   return redis.call('del', KEYS[1]) " +
                            "else " +
                            "   return 0 " +
                            "end";

            RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(key), value);
            return result != null && result == 1L;
        } catch (Exception ex) {
            log.error("releaseDistributedLock key={} error", key, ex);
            throw new RuntimeException(ex);
        }
    }

    // ==================== Hash Operations ====================

    public boolean hset(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
        return true;
    }

    public Map<String, String> hgetAll(String key) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        if (raw == null || raw.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return result;
    }

    public Long hdel(String key, String field) {
        return redisTemplate.opsForHash().delete(key, field);
    }

    // ==================== Generic set (Object) ====================

    public boolean set(String key, Object value, int ex) {
        return setEx(key, String.valueOf(value), ex);
    }

    public boolean set(String key, Object value) {
        return set(key, String.valueOf(value));
    }
}
