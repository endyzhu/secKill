package com.amos.ms.util;

import java.util.Collection;

/**
 * String操作类
 * @author zhuqb
 *
 */
public class StringUtils {
    /**
     * 判断是否为空
     *
     * @param obj
     * @return
     */
    public static boolean isBlank(Object obj) {
        if (null == obj) {
            return true;
        }

        if (obj instanceof Collection) {
            return ((Collection) obj).size() == 0 ? true : false;
        }

        return ("".equals(obj.toString()) || "null".equals(obj.toString())) ? true : false;
    }

    /**
     * 判断是否不为空
     * @param obj
     * @return
     */
    public static boolean isNotBlank(Object obj) {
        return !isBlank(obj);
    }
}
