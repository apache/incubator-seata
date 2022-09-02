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
package io.seata.rm.datasource.exec.handler.after;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.exec.constant.SQLTypeConstant;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLType;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author: lyx
 */
@LoadLevel(name = SQLTypeConstant.INSERT_IGNORE)
public class InsertIgnoreHandler extends BaseAfterHandler {

    @Override
    public Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        List<Row> beforeRow = beforeImage.getRows();
        Iterator<Row> iterator = afterImage.getRows().iterator();
        // filter exist images from before images
        while (iterator.hasNext()) {
            Row next = iterator.next();
            for (Row row : beforeRow) {
                if (row.equals(next)) {
                    iterator.remove();
                    break;
                }
            }
        }
        return Collections.singletonMap(SQLType.INSERT, afterImage.getRows());
    }
}
