package com.amos.ms.aspect;

import com.amos.ms.excepiton.CacheLockException;
import com.amos.ms.service.CacheLockService;
import com.amos.ms.type.CacheLock;
import com.amos.ms.util.CacheLockUtils;
import com.amos.ms.util.LockUtils;
import com.amos.ms.util.StringUtils;
import lombok.Data;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 针对 含有 @CacheLock 注解的方法 进行切入
 *
 * @author zhuqb
 */
@Component
@Aspect
@ComponentScan
@EnableAspectJAutoProxy
public class AspectAop {

    public static Logger logger = LoggerFactory.getLogger(AspectAop.class);

    @Autowired
    CacheLockService cacheLockService;

    /**
     * 对所有方法注解了CacheLock 进行拦截
     */
    @Pointcut("@annotation(com.amos.ms.type.CacheLock)")
    public void intercepter() {

    }

    /**
     * 方法执行前进行加锁
     *
     * @param joinPoint
     */
    @Before("intercepter()")
    public void doBeforeAdvice(JoinPoint joinPoint) {
        logger.info("这是前置通知");
        MethodParams params = this.getValuesFromMethod(joinPoint);
        // 加锁
        boolean lock = cacheLockService.lock(params.getKey(), params.getTimeout(), params.getExpireTime());
        if (!lock) {
            CacheLockUtils.count++;
            logger.info("获取锁失败");
            // 这里不能抛出异常 否则会造成程序死锁  不知道为什么
//            throw new CacheLockException("获取锁失败");
        }
    }

    /**
     * 后置通知  只要方法执行完成了 就会执行该操作
     *
     * @param joinPoint
     */
    @After("intercepter()")
    public void doAfterAdvice(JoinPoint joinPoint) {
        logger.info("这是后置通知");
        MethodParams params = this.getValuesFromMethod(joinPoint);
        cacheLockService.unlock(params.getKey());
    }

    /**
     * 获取锁操作 需要的参数
     *
     * @param joinPoint
     * @return
     */
    private MethodParams getValuesFromMethod(JoinPoint joinPoint) {
        // 获取所有的参数值
        Object[] paramVaules = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取 @CacheLock
        Annotation annotation = method.getAnnotation(CacheLock.class);

        // 拦截的参数
        String key = CacheLockUtils.getCacheLockKey(method,paramVaules);
        if (StringUtils.isBlank(key)) {
            throw new CacheLockException("没有需要加锁的参数");
        }

        // 对拦截的参数进行封装
        String lockKey = LockUtils.getLockKey(((CacheLock) annotation).lockedPrefix(), key);

        MethodParams params = new MethodParams();
        params.setExpireTime(((CacheLock) annotation).expireTime());
        params.setTimeout(((CacheLock) annotation).timeOut());
        params.setPrefix(((CacheLock) annotation).lockedPrefix());
        params.setKey(lockKey);
        return params;
    }

    @Data
    private static class MethodParams {
        /**
         * 拦截锁前缀
          */

        private String prefix;
        /**
         * 拦截对象
         */
        private String key;
        /**
         * 轮询时间
         */
        private long timeout;
        /**
         * 过期时间
         */
        private long expireTime;

    }
}
