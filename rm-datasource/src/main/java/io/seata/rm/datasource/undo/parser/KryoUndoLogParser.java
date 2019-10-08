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
package io.seata.rm.datasource.undo.parser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * kryo serializer
 * @author jsbxyyx
 */
@LoadLevel(name = KryoUndoLogParser.NAME)
public class KryoUndoLogParser implements UndoLogParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(KryoUndoLogParser.class);

    public static final String NAME = "kryo";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        KryoSerializer kryoSerializer = KryoSerializerFactory.getInstance().get();
        try {
            return kryoSerializer.serialize(new BranchUndoLog());
        } finally {
            KryoSerializerFactory.getInstance().returnKryo(kryoSerializer);
        }
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        KryoSerializer kryoSerializer = KryoSerializerFactory.getInstance().get();
        try {
            return kryoSerializer.serialize(branchUndoLog);
        } finally {
            KryoSerializerFactory.getInstance().returnKryo(kryoSerializer);
        }
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        KryoSerializer kryoSerializer = KryoSerializerFactory.getInstance().get();
        try {
            return kryoSerializer.deserialize(bytes);
        } finally {
            KryoSerializerFactory.getInstance().returnKryo(kryoSerializer);
        }
    }


    @Override
    public String writeValueAsString(BranchUndoLog branchUndoLog) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            //类为空时，不要抛异常
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//            objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
            objectMapper.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
                    jgen.writeObject("");// 输出孔字符串
                }
            });
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(Number.class, ToStringSerializer.instance);
            simpleModule.addSerializer(Date.class, ToStringSerializer.instance);
            objectMapper.registerModule(simpleModule);

            String context = objectMapper.writeValueAsString(branchUndoLog);
            return context;
        } catch (JsonProcessingException e) {
            LOGGER.error("json encode exception, {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
