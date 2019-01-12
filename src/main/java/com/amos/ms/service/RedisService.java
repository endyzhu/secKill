package com.amos.ms.service;

/**
 * redis操作接口
 */
public interface RedisService {
    /**
     * 设置键
     *
     * @param key           键的名称
     * @param value         键的值
     * @param expireSeconds 过期时间
     * @return
     */
    boolean setnx(String key, String value,long expireSeconds);

    /**
     * 删除键
     *
     * @param key
     * @return
     */
    boolean del(String key);

    /**
     * 获取键的值
     * @param s
     * @return
     */
    String getKey(String s);
}
