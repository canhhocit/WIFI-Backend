package com.wificaptive.common.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Distributed Lock Service backed by Redis (SETNX with lease TTL).
 * Prevents race conditions and duplicate scheduler runs across multiple Kubernetes backend pods.
 * Gracefully falls back to local in-memory lock if Redis is unavailable or unconfigured.
 */
@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private final StringRedisTemplate redisTemplate;
    private final String nodeId;
    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public DistributedLockService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        String host = System.getenv("HOSTNAME");
        this.nodeId = (host != null && !host.isBlank()) ? host : UUID.randomUUID().toString();
    }

    /**
     * Attempts to acquire a distributed lock for the specified key with a lease duration.
     *
     * @param lockKey the lock identifier (e.g. "scheduler:location-ap-count")
     * @param duration how long the lock should be held before automatic expiry
     * @return true if the lock was acquired, false otherwise
     */
    public boolean tryLock(String lockKey, Duration duration) {
        String fullKey = "lock:" + lockKey;
        if (redisTemplate != null) {
            try {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(fullKey, nodeId, duration);
                return Boolean.TRUE.equals(acquired);
            } catch (Exception e) {
                log.warn("[DistributedLock] Redis error when acquiring lock for {}: {}. Falling back to local lock.", lockKey, e.getMessage());
            }
        }

        // Local fallback if Redis not connected
        ReentrantLock lock = localLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        return lock.tryLock();
    }

    /**
     * Releases the lock. Only releases if held by this node (or via fallback).
     */
    public void unlock(String lockKey) {
        String fullKey = "lock:" + lockKey;
        if (redisTemplate != null) {
            try {
                String currentOwner = redisTemplate.opsForValue().get(fullKey);
                if (nodeId.equals(currentOwner)) {
                    redisTemplate.delete(fullKey);
                }
                return;
            } catch (Exception e) {
                log.warn("[DistributedLock] Redis error when releasing lock for {}: {}", lockKey, e.getMessage());
            }
        }

        ReentrantLock lock = localLocks.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Helper to run a task only if the distributed lock can be acquired.
     * Automatically releases the lock when execution finishes or fails.
     *
     * @param lockKey lock identifier
     * @param duration lease duration
     * @param task the runnable task to execute
     * @return true if acquired and executed, false if skipped because lock was held by another pod
     */
    public boolean runWithLock(String lockKey, Duration duration, Runnable task) {
        if (!tryLock(lockKey, duration)) {
            log.debug("[DistributedLock] Skipped task [{}] - lock is held by another cluster instance.", lockKey);
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            unlock(lockKey);
        }
    }
}
