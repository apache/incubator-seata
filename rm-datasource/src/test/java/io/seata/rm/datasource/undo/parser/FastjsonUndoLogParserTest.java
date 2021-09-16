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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ValueFilter;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.BaseUndoLogParserTest;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Geng Zhang
 */
public class FastjsonUndoLogParserTest extends BaseUndoLogParserTest {

    FastjsonUndoLogParser parser = (FastjsonUndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, FastjsonUndoLogParser.NAME);

    @Override
    public UndoLogParser getParser() {
        return parser;
    }

    /**
     * disable super testTimestampEncodeAndDecode
     */
    @Override
    public void testTimestampEncodeAndDecode() {
        Timestamp encodeStamp = new Timestamp(System.currentTimeMillis());
        encodeStamp.setNanos(999999);
        SerializeConfig.getGlobalInstance().addFilter(Timestamp.class, new TimestampSerializer());
        byte[] encode = JSON.toJSONString(encodeStamp, SerializeConfig.getGlobalInstance()).getBytes();
    }

    @Test
    public void testWriteClassName() throws Exception {
        TableRecords beforeImage =  new TableRecords();
        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("t1");
        List<Row> rows = new ArrayList<>();
        Row row = new Row();
        Field field = new Field();
        field.setName("id");
        field.setKeyType(KeyType.PRIMARY_KEY);
        field.setType(Types.BIGINT);
        field.setValue(Long.valueOf("0"));
        row.add(field);
        field = new Field();
        field.setName("money");
        field.setType(Types.DECIMAL);
        field.setValue(BigDecimal.ONE);
        row.add(field);
        rows.add(row);
        afterImage.setRows(rows);

        SQLUndoLog sqlUndoLog00 = new SQLUndoLog();
        sqlUndoLog00.setSqlType(SQLType.INSERT);
        sqlUndoLog00.setTableName("table_name");
        sqlUndoLog00.setBeforeImage(beforeImage);
        sqlUndoLog00.setAfterImage(afterImage);

        BranchUndoLog originLog = new BranchUndoLog();
        originLog.setBranchId(123456L);
        originLog.setXid("xiddddddddddd");
        List<SQLUndoLog> logList = new ArrayList<>();
        logList.add(sqlUndoLog00);
        originLog.setSqlUndoLogs(logList);

        // start test
        byte[] bs = getParser().encode(originLog);

        String s = new String(bs);
        Assertions.assertTrue(s.contains("\"@type\""));

        BranchUndoLog decode = getParser().decode(s.getBytes());
        Object value1 = decode.getSqlUndoLogs().get(0).getAfterImage().getRows().get(0).getFields().get(0).getValue();
        Object value2 = decode.getSqlUndoLogs().get(0).getAfterImage().getRows().get(0).getFields().get(1).getValue();
        Assertions.assertTrue(value1 instanceof Long);
        Assertions.assertTrue(value2 instanceof BigDecimal);
    }

    private class TimestampSerializer implements ValueFilter {

        @Override
        public Object process(Object object, String name, Object value) {
            return null;
        }
    }
}