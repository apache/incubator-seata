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

import com.google.protobuf.MessageLite;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author leizhiyuan
 */
public class ProtobufHelper {

    /**
     * Cache of parseFrom method
     */
    ConcurrentMap<Class, Method> parseFromMethodMap = new ConcurrentHashMap<>();

    /**
     * Cache of toByteArray method
     */
    ConcurrentMap<Class, Method> toByteArrayMethodMap = new ConcurrentHashMap<>();

    /**
     *  {className:class}
     */
    private ConcurrentMap<String, Class> requestClassCache = new ConcurrentHashMap<>();

    /**
     *
     * @param clazzName class name
     * @return the protobuf class
     */
    public Class getPbClass(String clazzName) {
        return CollectionUtils.computeIfAbsent(requestClassCache, clazzName, key -> {
            // get the parameter and result
            Class clazz;
            try {
                clazz = Class.forName(clazzName);
            } catch (ClassNotFoundException e) {
                throw new ShouldNeverHappenException("get class occurs exception", e);
            }
            if (clazz == void.class || !isProtoBufMessageClass(clazz)) {
                throw new ShouldNeverHappenException("class based protobuf: " + clazz.getName()
                        + ", only support return protobuf message!");
            }
            return clazz;
        });
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
