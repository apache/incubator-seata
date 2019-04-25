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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.seata.common.exception.ShouldNeverHappenException;

/**
 * @author leizhiyuan

 */
public class FrameSerialzer {

    private static final FramebufHelper PROTOBUF_HELPER = new FramebufHelper();

    /**
     * Encode method name
     */
    private static final String METHOD_TOBYTEARRAY = "toByteArray";
    /**
     * Decode method name
     */
    private static final String METHOD_PARSEFROM = "parseFrom";

    public static byte[] serializeContent(Object request) {

        Class clazz = request.getClass();
        Method method = PROTOBUF_HELPER.toByteArrayMethodMap.get(clazz);
        if (method == null) {
            try {
                method = clazz.getMethod(METHOD_TOBYTEARRAY);
                method.setAccessible(true);
                PROTOBUF_HELPER.toByteArrayMethodMap.put(clazz, method);
            } catch (Exception e) {
                throw new ShouldNeverHappenException("Cannot found method " + clazz.getName()
                    + ".toByteArray(), please check the generated code.", e);
            }
        }
        byte[] bytes = new byte[0];
        try {
            bytes = (byte[])method.invoke(request);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    public static <T> T deserializeContent(String responseClazz, byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        Class clazz = null;
        try {
            clazz = Class.forName(responseClazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Method method = PROTOBUF_HELPER.parseFromMethodMap.get(clazz);
        if (method == null) {
            try {
                method = clazz.getMethod(METHOD_PARSEFROM, byte[].class);
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new ShouldNeverHappenException("Cannot found static method " + clazz.getName()
                        + ".parseFrom(byte[]), please check the generated code");
                }
                method.setAccessible(true);
                PROTOBUF_HELPER.parseFromMethodMap.put(clazz, method);
            } catch (NoSuchMethodException e) {
                throw new ShouldNeverHappenException("Cannot found method " + clazz.getName()
                    + ".parseFrom(byte[]), please check the generated code", e);
            }
        }
        Object result;
        try {
            result = method.invoke(null, content);
        } catch (Exception e) {
            throw new ShouldNeverHappenException("Error when invoke " + clazz.getName() + ".parseFrom(byte[]).", e);
        }

        return (T)result;
    }
}