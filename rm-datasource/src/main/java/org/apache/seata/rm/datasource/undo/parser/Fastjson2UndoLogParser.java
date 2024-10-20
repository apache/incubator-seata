package org.apache.seata.rm.datasource.undo.parser;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import org.apache.seata.common.Constants;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;

import java.sql.Timestamp;

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
