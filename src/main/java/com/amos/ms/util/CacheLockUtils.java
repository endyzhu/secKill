package com.amos.ms.util;

import com.amos.ms.excepiton.CacheLockException;
import com.amos.ms.type.LockedComplexObject;
import com.amos.ms.type.LockedObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 缓存锁操作类
 *
 * @author zhuqb
 */
public class CacheLockUtils {

    public static Integer count = 0;
    /**
     * 获取拦截方法的拦截key
     *
     * @param method
     * @return
     */
    public static String getCacheLockKey(Method method,Object[] args) {
        // 获取方法中的注解参数
        Annotation[][] annotations = method.getParameterAnnotations();
        // 根据获取到的参数注解和参数列表获得加锁的参数
        Object lockedObject = getLockedObject(annotations, args);
        return lockedObject.toString();
    }

    /**
     * 根据获取到的参数注解和参数列表获得加锁的参数
     * 不支持多个参数加锁，只支持第一个注解为lockedObject或者lockedComplexObject的参数
     *
     * @param annotations
     * @param args
     * @return
     */
    public static Object getLockedObject(Annotation[][] annotations, Object[] args) {
        if (StringUtils.isBlank(annotations)) {
            throw new CacheLockException("没有被注解的参数");
        }

        if (StringUtils.isBlank(args)) {
            throw new CacheLockException("方法参数为空,没有被锁定的对象");
        }

        // 不支持多个参数加锁，只支持第一个注解为lockedObject或者lockedComplexObject的参数
        // 标记参数的位置指针
        int index = -1;

        for (int i = 0; i < annotations.length; i++) {
            for (int j = 0; j < annotations[i].length; j++) {
                // 注解为LockedComplexObject
                if (annotations[i][j] instanceof LockedComplexObject) {
                    index = i;
                    try {
                        return args[i].getClass().getField(((LockedComplexObject) annotations[i][j]).field());
                    } catch (NoSuchFieldException | SecurityException e) {
                        throw new CacheLockException("注解对象中没有该属性" + ((LockedComplexObject) annotations[i][j]).field());
                    }
                }

                if (annotations[i][j] instanceof LockedObject) {
                    index = i;
                    break;
                }
            }
            // 找到第一个后直接break，不支持多参数加锁
            if (index != -1) {
                break;
            }
        }
        if (index == -1) {
            throw new CacheLockException("请指定被锁定参数");
        }

        return args[index];
    }
}
