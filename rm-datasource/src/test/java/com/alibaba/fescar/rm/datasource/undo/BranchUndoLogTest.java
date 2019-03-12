/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.datasource.undo;

import java.sql.Types;
import java.util.ArrayList;

import com.alibaba.fescar.rm.datasource.sql.SQLType;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.Row;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

import org.junit.Assert;
import org.junit.Test;

/**
 * The type Branch undo log test.
 */
public class BranchUndoLogTest {

    /**
     * Test encode undo log.
     */
    @Test
    public void testEncodeUndoLog() {
        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setBranchId(641789253L);
        branchUndoLog.setXid("xid:xxx");

        ArrayList<SQLUndoLog> items = new ArrayList<>();
        SQLUndoLog item = new SQLUndoLog();
        item.setSqlType(SQLType.UPDATE);

        TableMeta tableMeta = new TableMeta();
        tableMeta.setTableName("product");

        TableRecords beforeImage = new TableRecords(tableMeta);
        Row rowb = new Row();
        rowb.add(new Field("id", Types.INTEGER, 1));
        rowb.add(new Field("name", Types.VARCHAR, "FESCAR"));
        rowb.add(new Field("since", Types.VARCHAR, "2014"));
        beforeImage.add(rowb);
        item.setBeforeImage(beforeImage);

        TableRecords afterImage = new TableRecords(tableMeta);
        Row rowa = new Row();
        rowa.add(new Field("id", Types.INTEGER, 1));
        rowa.add(new Field("name", Types.VARCHAR, "GTS"));
        rowa.add(new Field("since", Types.VARCHAR, "2014"));
        afterImage.add(rowa);
        item.setAfterImage(afterImage);

        items.add(item);

        branchUndoLog.setSqlUndoLogs(items);

        String encodeString = UndoLogParserFactory.getInstance().encode(branchUndoLog);
        System.out.println(encodeString);

        BranchUndoLog decodeObj = UndoLogParserFactory.getInstance().decode(encodeString);
        Assert.assertEquals(decodeObj.getBranchId(), branchUndoLog.getBranchId());

    }
}
