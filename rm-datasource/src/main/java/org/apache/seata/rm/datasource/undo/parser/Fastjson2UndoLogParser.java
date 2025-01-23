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
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;

@LoadLevel(name = Fastjson2UndoLogParser.NAME)
public class Fastjson2UndoLogParser implements UndoLogParser, Initialize {
    public static final String NAME = "fastjson2";

    private JSONReader.Feature[] jsonReaderFeature;
    private JSONWriter.Feature[] jsonWriterFeature;
    @Override
    public void init() {
        jsonReaderFeature = new JSONReader.Feature[]{
            JSONReader.Feature.UseDefaultConstructorAsPossible,
            // If not configured, it will be serialized based on public field and getter methods by default.
            // After configuration, it will be deserialized based on non-static fields (including private).
            // It will be safer under FieldBased configuration
            JSONReader.Feature.FieldBased,
            JSONReader.Feature.IgnoreAutoTypeNotMatch,
            JSONReader.Feature.UseNativeObject,
            JSONReader.Feature.SupportAutoType
        };

        jsonWriterFeature = new JSONWriter.Feature[]{
            JSONWriter.Feature.WriteClassName,
            JSONWriter.Feature.FieldBased,
            JSONWriter.Feature.ReferenceDetection,
            JSONWriter.Feature.WriteNulls,
            JSONWriter.Feature.NotWriteDefaultValue,
            JSONWriter.Feature.NotWriteHashMapArrayListClassName,
            JSONWriter.Feature.WriteNameAsSymbol
        };
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
        return JSONB.toBytes(branchUndoLog, jsonWriterFeature);
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        return JSONB.parseObject(bytes, BranchUndoLog.class, jsonReaderFeature);
    }

}
