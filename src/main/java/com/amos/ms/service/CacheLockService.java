package com.amos.ms.service;

/**
 * 锁操作接口
 * @author zhuqb
 */
public interface CacheLockService {
    /**
     * 给键添加锁，并且设置轮询时间和过期时间
     * @param key
     * @param timeout 轮询时间
     * @param expireSeconds 过期时间
     * @return
     */
    boolean lock(String key, long timeout, long expireSeconds);

    /**
     * 解锁
     * @param key
     * @return
     */
    boolean unlock(String key);

    /**
     * 是否被锁
     * @param key
     * @return
     */
    boolean isLock(String key);
}
