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
package io.seata.benchmark.undolog;

import io.seata.rm.datasource.sql.SQLType;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.rm.datasource.undo.parser.FastjsonUndoLogParser;
import io.seata.rm.datasource.undo.parser.JacksonUndoLogParser;
import io.seata.rm.datasource.undo.parser.KryoUndoLogParser;
import io.seata.rm.datasource.undo.parser.ProtostuffUndoLogParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ggndnn
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class UndoLogBench {
    @Param({"fastjson", "jackson", "kryo", "protostuff"})
    public String type;

    private BranchUndoLog undoLog;

    private UndoLogParser parser;

    @Setup(Level.Trial)
    public void setup() {
        switch (type) {
            case "fastjson":
                parser = new FastjsonUndoLogParser();
                break;
            case "jackson":
                parser = new JacksonUndoLogParser();
                break;
            case "kryo":
                parser = new KryoUndoLogParser();
                break;
            case "protostuff":
                parser = new ProtostuffUndoLogParser();
                break;
            default:
                break;
        }
        TableMeta tableMeta = new TableMeta();
        tableMeta.setTableName("t1");
        undoLog = new BranchUndoLog();
        undoLog.setBranchId(123456L);
        undoLog.setXid("xid00000");
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableName(tableMeta.getTableName());
        sqlUndoLog.setTableMeta(tableMeta);
        Map<String, String> rowData = new LinkedHashMap<>();
        rowData.put("f1", "1");
        rowData.put("f2", "1");
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(rowData);
        TableRecords beforeImage = createTableRecords(tableMeta, rows);
        rowData.put("f1", "2");
        rowData.put("f2", "2");
        TableRecords afterImage = createTableRecords(tableMeta, rows);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        List<SQLUndoLog> sqlUndoLogs = new ArrayList<>();
        sqlUndoLogs.add(sqlUndoLog);
        undoLog.setSqlUndoLogs(sqlUndoLogs);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
    }

    @Benchmark
    public void benchmark() {
        byte[] data = parser.encode(undoLog);
        parser.decode(data);
    }

    private TableRecords createTableRecords(TableMeta tableMeta, List<Map<String, String>> rows) {
        TableRecords result = new TableRecords();
        List<Row> rowList = new ArrayList<>();
        for (Map<String, String> rowData : rows) {
            List<Field> fields = new ArrayList<>();
            for (String fieldName : rowData.keySet()) {
                Field field = new Field();
                field.setType(JDBCType.VARCHAR.getVendorTypeNumber());
                field.setName(fieldName);
                field.setValue(rowData.get(fieldName));
                fields.add(field);
            }
            if (fields.size() > 0) {
                fields.get(0).setKeyType(KeyType.PrimaryKey);
            }
            Row row = new Row();
            row.setFields(fields);
            rowList.add(row);
        }
        result.setRows(rowList);
        result.setTableMeta(tableMeta);
        result.setTableName(tableMeta.getTableName());
        return result;
    }
}
