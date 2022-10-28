/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.tcc.serializer;

import io.seata.rm.tcc.serializer.spi.FastJsonContextSerializer;

/**
 * Used to serialize and deserialize BusinessActionContext
 *
 * @author zouwei
 */
public class BusinessActionContextSerializer {

    /**
     * serialize Object value to json string by jackson
     * 
     * @param value
     * @return
     */
    public static String toJsonString(Object value) {
        ContextSerializer contextSerializer = ContextSerializerFactory.getInstance();
        return contextSerializer.encodeToString(value);
    }

    /**
     * deserialize json string by fastjson or jackson
     * 
     * @param json json string
     * @param clazz target class
     * @param <T>
     * @return
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        T result;
        // json string start with "{"@class":", it will deserialize by jackson
        if (json.startsWith("{\"@class\":")) {
            ContextSerializer defaultContextSerializer = ContextSerializerFactory.getInstance();
            result = defaultContextSerializer.decodeString(json, clazz);
        } else {
            ContextSerializer fastjsonContextSerializer =
                ContextSerializerFactory.getInstance(FastJsonContextSerializer.NAME);
            result = fastjsonContextSerializer.decodeString(json, clazz);
        }
        return result;
    }
}
