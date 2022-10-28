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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.seata.common.util.StringUtils;

/**
 * abstract context serializer
 *
 * @author zouwei
 */
public abstract class AbstractContextSerializer implements ContextSerializer {

    /**
     * encode object to bytes
     * 
     * @param value
     * @return
     */
    @Override
    public byte[] encode(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return doEncode(value);
    }

    /**
     * encode object to string
     * 
     * @param value
     * @return
     */
    @Override
    public String encodeToString(Object value) {
        byte[] bytes = encode(value);
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        return new String(encode(value), StandardCharsets.UTF_8);
    }

    /**
     * custom encode
     *
     * @param value
     * @return
     */
    protected abstract byte[] doEncode(Object value);

    /**
     * decode bytes to target class
     *
     * @param bytes
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length <= 0 || Objects.isNull(clazz)) {
            return null;
        }
        return doDecode(bytes, clazz);
    }

    /**
     * decode string to target class
     * 
     * @param string
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public <T> T decodeString(String string, Class<T> clazz) {
        if (StringUtils.isBlank(string) || Objects.isNull(clazz)) {
            return null;
        }
        return decode(string.getBytes(StandardCharsets.UTF_8), clazz);
    }

    /**
     * custom decode
     * 
     * @param bytes
     * @param clazz
     * @param <T>
     * @return
     */
    protected abstract <T> T doDecode(byte[] bytes, Class<T> clazz);
}
