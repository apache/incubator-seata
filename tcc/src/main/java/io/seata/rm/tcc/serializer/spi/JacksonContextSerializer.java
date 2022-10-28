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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.tcc.serializer.AbstractContextSerializer;

/**
 * BusinessActionContext serialize by jackson serializer
 *
 * @author zouwei
 */
@LoadLevel(name = JacksonContextSerializer.NAME)
public class JacksonContextSerializer extends AbstractContextSerializer implements Initialize {

    public static final String NAME = "jackson";

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonContextSerializer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init() {
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.activateDefaultTyping(this.mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        this.mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * serialize by jackson
     *
     * @param value
     * @return
     */
    @Override
    protected byte[] doEncode(Object value) {
        try {
            return this.mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            LOGGER.error("json toJsonString exception, {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * deserialize by jackson
     *
     * @param bytes
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    protected <T> T doDecode(byte[] bytes, Class<T> clazz) {
        try {
            return this.mapper.readValue(bytes, clazz);
        } catch (IOException e) {
            LOGGER.error("json parseObject exception, {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
