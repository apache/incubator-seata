/**
 * Copyright Notice: This software is developed by Ant Small and Micro Financial Services Group Co., Ltd. This software
 * and
 * all the relevant information, including but not limited to any signs, images, photographs, animations, text,
 * interface design, audios and videos, and printed materials, are protected by copyright laws and other intellectual
 * property laws and treaties.
 *
 * The use of this software shall abide by the laws and regulations as well as Software Installation License
 * Agreement/Software Use Agreement updated from time to time. Without authorization from Ant Small and Micro Financial
 * Services Group Co., Ltd., no one may conduct the following actions:
 *
 * 1) reproduce, spread, present, set up a mirror of, upload, download this software;
 *
 * 2) reverse engineer, decompile the source code of this software or try to find the source code in any other ways;
 *
 * 3) modify, translate and adapt this software, or develop derivative products, works, and services based on this
 * software;
 *
 * 4) distribute, lease, rent, sub-license, demise or transfer any rights in relation to this software, or authorize
 * the reproduction of this software on other’s computers.
 */
package io.seata.core.protocol.serialize;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.protobuf.MessageLite;
import io.seata.common.exception.ShouldNeverHappenException;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FramebufHelper {

    /**
     * Cache of parseFrom method
     */
    ConcurrentMap<Class, Method> parseFromMethodMap = new ConcurrentHashMap<Class, Method>();

    /**
     * Cache of toByteArray method
     */
    ConcurrentMap<Class, Method> toByteArrayMethodMap = new ConcurrentHashMap<Class, Method>();

    /**
     * 请求参数类型缓存 {service+method:class}
     */
    private ConcurrentMap<String, Class> requestClassCache = new ConcurrentHashMap<String, Class>();

    /**
     * 从缓存中获取请求值类
     *
     * @param service    接口名
     * @return 请求参数类
     */
    public Class getPbClass(String service) {
        Class reqClass = requestClassCache.get(service);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            Class clazz = null;
            try {
                clazz = Class.forName(service);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            loadProtoClassToCache(service, clazz);
        }
        return requestClassCache.get(service);
    }

    /**
     * 加载protobuf接口里方法的参数和返回值类型到缓存，不需要传递
     *
     * @param key        缓存的key
     * @param clazz      接口名
     */
    private void loadProtoClassToCache(String key, Class clazz) {
        if (clazz == void.class || !isProtoBufMessageClass(clazz)) {
            throw new ShouldNeverHappenException("class based protobuf: " + clazz.getName()
                + ", only support return protobuf message!");
        }
        requestClassCache.put(key, clazz);
    }

    /**
     * Is this class is assignable from MessageLite
     *
     * @param clazz unknown class
     * @return is assignable from MessageLite
     */
    boolean isProtoBufMessageClass(Class clazz) {
        return clazz != null && MessageLite.class.isAssignableFrom(clazz);
    }
}
