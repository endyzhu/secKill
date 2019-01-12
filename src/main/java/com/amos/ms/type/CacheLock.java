package com.amos.ms.type;

import java.lang.annotation.*;

/**
 * 方法级注解
 *
 * @author zhuqb
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheLock {
    /**
     * redis 锁key的前缀
     *
     * @return
     */
    String lockedPrefix() default "";

    /**
     * 轮询锁的时间 默认是2s
     *
     * @return
     */
    long timeOut() default 2000;

    /**
     * key在redis里存在的时间，1000S
     *
     * @return
     */
    int expireTime() default 1000;//

}
