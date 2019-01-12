package com.amos.ms.intercepter;

import com.amos.ms.excepiton.CacheLockException;
import com.amos.ms.service.CacheLockService;
import com.amos.ms.type.CacheLock;
import com.amos.ms.type.LockedComplexObject;
import com.amos.ms.type.LockedObject;
import com.amos.ms.util.CacheLockUtils;
import com.amos.ms.util.LockUtils;
import com.amos.ms.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.InvocationHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 使用代理模式获取锁注解的方法和参数，在执行方法之前加锁  在执行方法之后解锁
 *
 * @author zhuqb
 */
public class CacheLockIncepter implements InvocationHandler {
    private static Logger logger = LoggerFactory.getLogger(CacheLockIncepter.class);
    @Autowired
    CacheLockService cacheLockService;
    private Object proxied;
    public CacheLockIncepter(Object proxied) {
        this.proxied = proxied;
    }
    public static int ERROR_COUNT  = 0;

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        // 获取方法中是否含有CacheLock方法注解
        Annotation annotation = method.getAnnotation(CacheLock.class);
        // 没有注解 直接放行
        if (StringUtils.isBlank(annotation)) {
            logger.info(method.getName() + "方法不含注解CacheLock");
            return method.invoke(proxied, objects);
        }

        // 获取方法中的注解参数
        Annotation[][] annotations = method.getParameterAnnotations();
        // 根据获取到的参数注解和参数列表获得加锁的参数
        Object lockedObject = CacheLockUtils.getLockedObject(annotations, objects);

        String objectValue = lockedObject.toString();
        String lockKey = LockUtils.getLockKey(((CacheLock) annotation).lockedPrefix(), objectValue.toString());
        // 加锁
        boolean lock = cacheLockService.lock(lockKey, ((CacheLock) annotation).timeOut(), ((CacheLock) annotation).expireTime());
        if (!lock) {
            ERROR_COUNT ++;
            throw new CacheLockException("获取锁失败");
        }
        try {
            // 执行方法
            return method.invoke(proxied, objects);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
        } finally {
            cacheLockService.unlock(lockKey);
        }
        return null;
    }

}
