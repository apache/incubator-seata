/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.undo.parser;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.json.Fastjson2JsonbParser;
import org.apache.seata.common.json.Fastjson2ObjectReaderWarmup;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.sqlparser.SQLType;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;

@LoadLevel(name = Fastjson2UndoLogParser.NAME)
public class Fastjson2UndoLogParser implements UndoLogParser, Initialize {
    public static final String NAME = "fastjson2";

    private JSONReader.Feature[] jsonReaderFeature;
    private JSONWriter.Feature[] jsonWriterFeature;

    /**
     * Parser-local writer provider, so the Timestamp writer below doesn't change how fastjson2 writes
     * Timestamp anywhere else in the application (e.g. the RPC serializer).
     */
    private final ObjectWriterProvider writerProvider = new ObjectWriterProvider();

    @Override
    public void init() {
        jsonReaderFeature = new JSONReader.Feature[] {
            JSONReader.Feature.UseDefaultConstructorAsPossible,
            // If not configured, it will be serialized based on public field and getter methods by default.
            // After configuration, it will be deserialized based on non-static fields (including private).
            // It will be safer under FieldBased configuration
            JSONReader.Feature.FieldBased,
            JSONReader.Feature.IgnoreAutoTypeNotMatch,
            JSONReader.Feature.UseNativeObject,
            JSONReader.Feature.SupportAutoType
        };

        jsonWriterFeature = new JSONWriter.Feature[] {
            JSONWriter.Feature.WriteClassName,
            JSONWriter.Feature.FieldBased,
            JSONWriter.Feature.ReferenceDetection,
            JSONWriter.Feature.WriteNulls,
            JSONWriter.Feature.NotWriteDefaultValue,
            JSONWriter.Feature.NotWriteHashMapArrayListClassName,
            JSONWriter.Feature.WriteNameAsSymbol
        };

        // JSONB writes java.sql.Timestamp without its class name by default, so the value is decoded as LocalDateTime
        // and no longer equals the Timestamp read from the database when the undo images are validated.
        writerProvider.register(Timestamp.class, new TimestampWriter(), true);

        Fastjson2ObjectReaderWarmup.warmup(
                Object.class,
                ArrayList.class,
                BranchUndoLog.class,
                SQLUndoLog.class,
                SQLType.class,
                TableRecords.class,
                Row.class,
                Field.class);

        // SerialArray support: Fastjson2 with FieldBased and SupportAutoType features
        // can handle SerialArray serialization automatically through field access
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return encode(new BranchUndoLog());
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        return JSONB.toBytes(branchUndoLog, new JSONWriter.Context(writerProvider, jsonWriterFeature));
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        return Fastjson2JsonbParser.parseObject(bytes, BranchUndoLog.class, jsonReaderFeature);
    }

    /**
     * Writes the class name before the value, so fastjson2's built-in Timestamp reader decodes it back to a
     * Timestamp, nanos included. Undo logs written this way are still readable by clients without this writer.
     */
    static class TimestampWriter implements ObjectWriter<Timestamp> {

        @Override
        public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
            jsonWriter.writeLocalDateTime(((Timestamp) object).toLocalDateTime());
        }

        @Override
        public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
            jsonWriter.writeTypeName(Timestamp.class.getName());
            jsonWriter.writeLocalDateTime(((Timestamp) object).toLocalDateTime());
        }
    }
}
