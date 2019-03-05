package com.alibaba.fescar.common.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author zhangsen
 */
public class ProxyUtils {

    /**
     * 获取类所有的接口
     * @param clazz
     * @return
     */
    public static Set<Class<?>> getInterfaces(Class<?> clazz){
        if (clazz.isInterface() ) {
            return Collections.<Class<?>>singleton(clazz);
        }
        Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        while (clazz != null) {
            Class<?>[] ifcs = clazz.getInterfaces();
            for (Class<?> ifc : ifcs) {
                interfaces.addAll(getInterfaces(ifc));
            }
            clazz = clazz.getSuperclass();
        }
        return interfaces;
    }

}
