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

import io.protostuff.Input;
import io.protostuff.LinkedBuffer;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.WireFormat.FieldType;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;

import java.io.IOException;

/**
 * The type protostuff based undo log parser.
 *
 * @author Geng Zhang
 */
@LoadLevel(name = ProtostuffUndoLogParser.NAME)
public class ProtostuffUndoLogParser implements UndoLogParser {

    public static final String NAME = "protostuff";
    
    private final static DefaultIdStrategy ID_STRATEGY = ((DefaultIdStrategy) RuntimeEnv.ID_STRATEGY);

    static {
        ID_STRATEGY.registerDelegate(new DateDelegate());
        ID_STRATEGY.registerDelegate(new TimestampDelegate());
        ID_STRATEGY.registerDelegate(new SqlDateDelegate());
        ID_STRATEGY.registerDelegate(new TimeDelegate());
    }

    private static final Schema<BranchUndoLog> SCHEMA = RuntimeSchema.getSchema(BranchUndoLog.class);

    @Override
    public String getName() {
        return ProtostuffUndoLogParser.NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return new byte[0];
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        // Re-use (manage) this buffer to avoid allocating on every serialization
        LinkedBuffer buffer = LinkedBuffer.allocate(512);
        // ser
        try {
            return ProtostuffIOUtil.toByteArray(branchUndoLog, SCHEMA, buffer);
        } finally {
            buffer.clear();
        }
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        if (bytes.length == 0) {
            return new BranchUndoLog();
        }
        BranchUndoLog fooParsed = SCHEMA.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, fooParsed, SCHEMA);
        return fooParsed;
    }

    /**
     * Delegate for java.sql.Timestamp
     * 
     * @author zhangsen
     */
    public static class TimestampDelegate implements Delegate<java.sql.Timestamp> {

        @Override
        public FieldType getFieldType() {
            return FieldType.FIXED64;
        }

        @Override
        public Class<?> typeClass() {
            return java.sql.Timestamp.class;
        }

        @Override
        public java.sql.Timestamp readFrom(Input input) throws IOException {
            return new java.sql.Timestamp(input.readFixed64());
        }

        @Override
        public void writeTo(Output output, int number, java.sql.Timestamp value, boolean repeated) throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }
    }

    /**
     * Delegate for java.sql.Date
     *
     * @author zhangsen
     */
    public static class SqlDateDelegate implements Delegate<java.sql.Date> {

        @Override
        public FieldType getFieldType() {
            return FieldType.FIXED64;
        }

        @Override
        public Class<?> typeClass() {
            return java.sql.Date.class;
        }

        @Override
        public java.sql.Date readFrom(Input input) throws IOException {
            return new java.sql.Date(input.readFixed64());
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

        @Override
        public void writeTo(Output output, int number, java.sql.Date value, boolean repeated) throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }
    }

    /**
     * Delegate for java.sql.Time
     *
     * @author zhangsen
     */
    public static class TimeDelegate implements Delegate<java.sql.Time> {

        @Override
        public FieldType getFieldType() {
            return FieldType.FIXED64;
        }

        @Override
        public Class<?> typeClass() {
            return java.sql.Time.class;
        }

        @Override
        public java.sql.Time readFrom(Input input) throws IOException {
            return new java.sql.Time(input.readFixed64());
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

        @Override
        public void writeTo(Output output, int number, java.sql.Time value, boolean repeated) throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }
    }

    /**
     * Delegate for java.util.Date
     *
     * @author zhangsen
     */
    public static class DateDelegate implements Delegate<java.util.Date> {

        @Override
        public FieldType getFieldType() {
            return FieldType.FIXED64;
        }

        @Override
        public Class<?> typeClass() {
            return java.util.Date.class;
        }

        @Override
        public java.util.Date readFrom(Input input) throws IOException {
            return new java.util.Date(input.readFixed64());
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

        @Override
        public void writeTo(Output output, int number, java.util.Date value, boolean repeated) throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }
    }
}
