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

import com.alibaba.druid.proxy.jdbc.ClobProxyImpl;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.seata.common.Constants;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import oracle.sql.CLOB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The type Json based undo log parser.
 *
 * @author jsbxyyx
 */
@LoadLevel(name = JacksonUndoLogParser.NAME)
public class JacksonUndoLogParser implements UndoLogParser {

    public static final String NAME = "jackson";

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonUndoLogParser.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final SimpleModule MODULE = new SimpleModule();

    /**
     * customize serializer for java.sql.Timestamp
     */
    private static final JsonSerializer TIMESTAMP_SERIALIZER = new TimestampSerializer();
//    private static final JsonSerializer JAVASQLCLOB_SERIALIZER = new JavaSqlClobSerializer();

    /**
     * customize deserializer for java.sql.Timestamp
     */
    private static final JsonDeserializer TIMESTAMP_DESERIALIZER = new TimestampDeserializer();

    /**
     * customize serializer of java.sql.Blob
     */
    private static final JsonSerializer BLOB_SERIALIZER = new BlobSerializer();

    /**
     * customize deserializer of java.sql.Blob
     */
    private static final JsonDeserializer BLOB_DESERIALIZER = new BlobDeserializer();

    /**
     * customize serializer of java.sql.Clob
     */
    private static final JsonSerializer CLOB_SERIALIZER = new ClobSerializer();

    /**
     * customize deserializer of java.sql.Clob
     */
    private static final JsonDeserializer CLOB_DESERIALIZER = new ClobDeserializer();

    static {
        MODULE.addSerializer(SerialBlob.class, BLOB_SERIALIZER);
        MODULE.addDeserializer(SerialBlob.class, BLOB_DESERIALIZER);
        MODULE.addSerializer(SerialClob.class, CLOB_SERIALIZER);
        MODULE.addDeserializer(SerialClob.class, CLOB_DESERIALIZER);

        MODULE.addSerializer(Timestamp.class, TIMESTAMP_SERIALIZER);
        MODULE.addDeserializer(Timestamp.class, TIMESTAMP_DESERIALIZER);
        MAPPER.registerModule(MODULE);

        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        MAPPER.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return "{}".getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(branchUndoLog);
            return bytes;
        } catch (JsonProcessingException e) {
            LOGGER.error("json encode exception, {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        try {
            BranchUndoLog branchUndoLog = MAPPER.readValue(bytes, BranchUndoLog.class);
            return branchUndoLog;
        } catch (IOException e) {
            LOGGER.error("json decode exception, {}", e.getMessage(), e);
            throw new RuntimeException(e);
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

    public static void main(String[] args) {

    }

    /**
     * if necessary
     * extend {@link ArraySerializerBase}
     */
    private static class TimestampSerializer extends JsonSerializer<Timestamp> {

        @Override
        public void serializeWithType(Timestamp timestamp, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSerializer) throws IOException {
            WritableTypeId typeId = typeSerializer.writeTypePrefix(gen, typeSerializer.typeId(timestamp, JsonToken.START_ARRAY));
            serialize(timestamp, gen, serializers);
            gen.writeTypeSuffix(typeId);
        }

        @Override
        public void serialize(Timestamp timestamp, JsonGenerator gen, SerializerProvider serializers) {
            try {
                gen.writeNumber(timestamp.getTime());
                gen.writeNumber(timestamp.getNanos());
            } catch (IOException e) {
                LOGGER.error("serialize java.sql.Timestamp error : {}", e.getMessage(), e);
            }

        }
    }

    /**
     * if necessary
     * extend {@link JsonNodeDeserializer}
     */
    private static class TimestampDeserializer extends JsonDeserializer<Timestamp> {

        @Override
        public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) {
            if(p.isExpectedStartArrayToken()){
                ArrayNode arrayNode;
                try {
                    arrayNode = p.getCodec().readTree(p);
                    Timestamp timestamp = new Timestamp(arrayNode.get(0).asLong());
                    timestamp.setNanos(arrayNode.get(1).asInt());
                    return timestamp;
                } catch (IOException e) {
                    LOGGER.error("deserialize java.sql.Timestamp error : {}", e.getMessage(), e);
                }
            }
            LOGGER.error("deserialize java.sql.Timestamp type error.");
            return null;
        }
    }

    /**
     * clob转string
     */
    private static class JavaSqlClobSerializer extends JsonSerializer<Clob> {

        @Override
        public void serializeWithType(Clob clob, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSerializer) throws IOException {
            ClobString clobs = new ClobString("");
            String str = new String("");
            WritableTypeId typeId = typeSerializer.writeTypePrefix(gen, typeSerializer.typeId(str, JsonToken.VALUE_STRING));
            serialize(clob, gen, serializers);
            gen.writeTypeSuffix(typeId);
        }


        @Override
        public void serialize(Clob clob, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            String clobToStr= "";
            Reader is = null;// 得到流
            try {
                is = clob.getCharacterStream();
            } catch (SQLException e) {
                LOGGER.error("OracleClobSerializer json encode exception, {}", e.getMessage(), e);
            }
            BufferedReader br = new BufferedReader(is);
            String s = br.readLine();
            StringBuffer sb = new StringBuffer();
            // 执行循环将字符串全部取出付值给StringBuffer由StringBuffer转成STRING
            while (s != null) {
                sb.append(s);
                s = br.readLine();
            }
            clobToStr = sb.toString();
            jgen.writeString(clobToStr);
        }
    }

    /**
     * the class of serialize blob type
     */
    private static class BlobSerializer extends JsonSerializer<SerialBlob> {

        @Override
        public void serializeWithType(SerialBlob blob, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(blob, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(blob, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(SerialBlob blob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try {
                gen.writeBinary(blob.getBytes(1, (int) blob.length()));
            } catch (SerialException e) {
                LOGGER.error("serialize java.sql.Blob error : {}", e.getMessage(), e);
            }
        }
    }

    /**
     * the class of deserialize blob type
     */
    private static class BlobDeserializer extends JsonDeserializer<SerialBlob> {

        @Override
        public SerialBlob deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            try {
                return new SerialBlob(p.getBinaryValue());
            } catch (SQLException e) {
                LOGGER.error("deserialize java.sql.Blob error : {}", e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * the class of serialize clob type
     */
    private static class ClobSerializer extends JsonSerializer<SerialClob> {

        @Override
        public void serializeWithType(SerialClob clob, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(clob, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(clob, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(SerialClob clob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try {
                gen.writeString(clob.getCharacterStream(), (int) clob.length());
            } catch (SerialException e) {
                LOGGER.error("serialize java.sql.Blob error : {}", e.getMessage(), e);
            }
        }
    }

    private static class ClobDeserializer extends JsonDeserializer<SerialClob> {

        @Override
        public SerialClob deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            try {
                return new SerialClob(p.getValueAsString().toCharArray());

            } catch (SQLException e) {
                LOGGER.error("deserialize java.sql.Clob error : {}", e.getMessage(), e);
            }
            return null;
        }
    }
}
