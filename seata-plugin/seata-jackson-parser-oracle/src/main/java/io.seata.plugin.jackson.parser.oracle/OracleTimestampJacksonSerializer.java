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
package io.seata.plugin.jackson.parser.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.parser.spi.JacksonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author jsbxyyx
 */
@LoadLevel(name = "oracleTimestamp")
public class OracleTimestampJacksonSerializer implements JacksonSerializer<oracle.sql.TIMESTAMP> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleTimestampJacksonSerializer.class);

    @Override
    public Class<oracle.sql.TIMESTAMP> type() {
        return oracle.sql.TIMESTAMP.class;
    }

    @Override
    public JsonSerializer<oracle.sql.TIMESTAMP> ser() {
        return new JsonSerializer<oracle.sql.TIMESTAMP>() {
            @Override
            public void serializeWithType(oracle.sql.TIMESTAMP timestamp, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSerializer) throws IOException {
                WritableTypeId typeId = typeSerializer.writeTypePrefix(gen, typeSerializer.typeId(timestamp, JsonToken.VALUE_EMBEDDED_OBJECT));
                serialize(timestamp, gen, serializers);
                gen.writeTypeSuffix(typeId);
            }

            @Override
            public void serialize(oracle.sql.TIMESTAMP timestamp, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                try {
                    gen.writeBinary(timestamp.getBytes());
                } catch (IOException e) {
                    LOGGER.error("serialize oralce.sql.Timestamp error : {}", e.getMessage(), e);
                }
            }
        };
    }

    @Override
    public JsonDeserializer<? extends oracle.sql.TIMESTAMP> deser() {
        return new JsonDeserializer<oracle.sql.TIMESTAMP>() {
            @Override
            public oracle.sql.TIMESTAMP deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                try {
                    oracle.sql.TIMESTAMP timestamp = new oracle.sql.TIMESTAMP(p.getBinaryValue());
                    return timestamp;
                } catch (IOException e) {
                    LOGGER.error("deserialize oracle.sql.Timestamp error : {}", e.getMessage(), e);
                }
                return null;
            }
        };
    }

}
