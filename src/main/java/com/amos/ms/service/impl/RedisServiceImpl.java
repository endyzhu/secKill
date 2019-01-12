package com.amos.ms.service.impl;

import com.amos.ms.intercepter.CacheLockIncepter;
import com.amos.ms.service.RedisService;
import com.amos.ms.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis操作类
 *
 * @author zhuqb
 */
@Service
public class RedisServiceImpl implements RedisService {

    private static Logger logger = LoggerFactory.getLogger(CacheLockIncepter.class);

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 设置键，并且设置过期时间
     *
     * @param key           键的名称
     * @param value         键的值
     * @param expireSeconds 过期时间 单位 ms
     * @return
     */
    @Override
    public boolean setnx(String key, String value, long expireSeconds) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key,value,expireSeconds,TimeUnit.MILLISECONDS);
    }

    /**
     * 删除键
     *
     * @param key
     * @return
     */
    @Override
    public boolean del(String key) {
        return stringRedisTemplate.delete(key);
    }

    @Override
    public String getKey(String s) {
        return stringRedisTemplate.opsForValue().get(s);
    }

}
