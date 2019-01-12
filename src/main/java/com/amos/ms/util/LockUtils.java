package com.amos.ms.util;

/**
 * 锁操作类
 *
 * @author zhuqb
 */
public class LockUtils {
    /**
     * 返回锁键
     *
     * @param prefix
     * @param key
     * @return
     */
    public static String getLockKey(String prefix, String key) {
        return prefix + "_" + key + "_lock";
    }
}
