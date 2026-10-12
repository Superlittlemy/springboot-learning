package com.slm.multilevelcache.consistency;

/**
 * 分布式锁
 *
 * <p>用于缓存重建互斥：热点 key 过期瞬间可能有成百上千请求同时回源数据库，
 * 用一把分布式锁把并发重建收敛成一次「回源 + 回填」，即防缓存击穿。</p>
 */
public interface DistributedLock {

    /**
     * 尝试加锁
     *
     * @param lockKey 锁标识
     * @param token   本次加锁的持有者标识，解锁时校验以避免误删他人的锁
     * @return 是否加锁成功
     */
    boolean tryLock(String lockKey, String token);

    /**
     * 释放锁。仅当锁的持有者与 token 一致时才真正删除。
     */
    void unlock(String lockKey, String token);

}