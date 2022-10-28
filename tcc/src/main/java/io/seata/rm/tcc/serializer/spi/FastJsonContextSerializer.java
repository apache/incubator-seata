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
package io.seata.rm.tcc.serializer.spi;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.alibaba.fastjson.JSON;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.tcc.serializer.AbstractContextSerializer;

/**
 * BusinessActionContext serialize by fastjson serializer
 *
 * @author zouwei
 */
@LoadLevel(name = FastJsonContextSerializer.NAME)
public class FastJsonContextSerializer extends AbstractContextSerializer {

    public static final String NAME = "fastjson";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * serialize by fastjson
     *
     * @param value
     * @return
     */
    @Override
    protected byte[] doEncode(Object value) {
        if (value instanceof String) {
            return ((String)value).getBytes(StandardCharsets.UTF_8);
        }
        return JSON.toJSONBytes(value);
    }

    /**
     * deserialize by fastjson
     *
     * @param bytes
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    protected <T> T doDecode(byte[] bytes, Class<T> clazz) {
        if (Objects.equals(clazz, byte[].class)) {
            return (T)bytes;
        }
        return Objects.equals(clazz, String.class) ? (T)new String(bytes, StandardCharsets.UTF_8)
            : JSON.parseObject(bytes, clazz);
    }
}
