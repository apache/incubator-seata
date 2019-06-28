/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.codec.protobuf;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.seata.common.exception.ShouldNeverHappenException;

/**
 * @author leizhiyuan
 */
public class ProtobufSerializer {

    private static final ProtobufHelper PROTOBUF_HELPER = new ProtobufHelper();

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
        } catch (Exception e) {
            throw new ShouldNeverHappenException("serialize occurs exception", e);
        }

        return bytes;
    }

    public static <T> T deserializeContent(String responseClazz, byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        Class clazz = PROTOBUF_HELPER.getPbClass(responseClazz);

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