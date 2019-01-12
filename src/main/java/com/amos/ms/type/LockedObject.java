package com.amos.ms.type;

import java.lang.annotation.*;

/**
 * 参数级注解
 *
 * @author zhuqb
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LockedObject {
    /**
     * 传入的参数值
     *
     * @return
     */
    String value() default "";
}
