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
package io.seata.serializer.protobuf;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author leizhiyuan
 */
public class ProtobufInnerSerializer {

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
        Method method = CollectionUtils.computeIfAbsent(PROTOBUF_HELPER.toByteArrayMethodMap, clazz, key -> {
            try {
                Method m = clazz.getMethod(METHOD_TOBYTEARRAY);
                m.setAccessible(true);
                return m;
            } catch (Exception e) {
                throw new ShouldNeverHappenException("Cannot found method " + clazz.getName()
                    + ".toByteArray(), please check the generated code.", e);
            }
        });

        try {
            return (byte[])method.invoke(request);
        } catch (Exception e) {
            throw new ShouldNeverHappenException("serialize occurs exception", e);
        }
    }

    public static <T> T deserializeContent(String responseClazz, byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        Class clazz = PROTOBUF_HELPER.getPbClass(responseClazz);

        Method method = CollectionUtils.computeIfAbsent(PROTOBUF_HELPER.parseFromMethodMap, clazz, key -> {
            try {
                Method m = clazz.getMethod(METHOD_PARSEFROM, byte[].class);
                if (!Modifier.isStatic(m.getModifiers())) {
                    throw new ShouldNeverHappenException("Cannot found static method " + clazz.getName()
                        + ".parseFrom(byte[]), please check the generated code");
                }
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                throw new ShouldNeverHappenException("Cannot found method " + clazz.getName()
                    + ".parseFrom(byte[]), please check the generated code", e);
            }
        });

        try {
            return (T)method.invoke(null, content);
        } catch (Exception e) {
            throw new ShouldNeverHappenException("Error when invoke " + clazz.getName() + ".parseFrom(byte[]).", e);
        }
    }
}