package com.amos.ms.service.impl;

import com.amos.ms.intercepter.CacheLockIncepter;
import com.amos.ms.service.CacheLockService;
import com.amos.ms.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 锁操作实现
 * @author zhuqb
 */
@Service
public class CacheLockServiceImpl implements CacheLockService {

    //纳秒和毫秒之间的转换率
    public static final long MILLI_NANO_TIME = 1000 * 1000L;

    private static Logger logger = LoggerFactory.getLogger(CacheLockIncepter.class);
    @Autowired
    RedisService redisService;

    /**
     * 给键添加锁
     *
     * @param key
     * @param timeout 轮询时间
     * @param expireSeconds 过期时间
     * @return
     */
    @Override
    public boolean lock(String key, long timeout, long expireSeconds) {
        boolean flag = false;
        long nanoTime = System.nanoTime();
        // 轮询时间
        timeout *= MILLI_NANO_TIME;

        try{
            while (System.nanoTime() - nanoTime < timeout) {
                if (redisService.setnx(key,System.currentTimeMillis()+"",expireSeconds)) {
                    return true;
                }
                logger.info("出现锁等待");
                // 短暂休眠，避免可能的活锁
                Thread.sleep(3);
            }
        }catch (Exception e) {
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            flag = false;
        }
        return flag;
    }

    /**
     * 释放锁
     *
     * @param key
     * @return
     */
    @Override
    public boolean unlock(String key) {
        return redisService.del(key);
    }

    @Override
    public boolean isLock(String key) {
        return false;
    }
}
