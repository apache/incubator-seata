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
package io.seata.rm.datasource.sql.struct.cache;

import com.mysql.cj.MysqlType;
import com.mysql.cj.jdbc.result.ResultSetMetaData;
import com.mysql.cj.result.Field;
import io.seata.common.loader.LoadLevel;
import io.seata.sqlparser.struct.ColumnMeta;

import java.sql.SQLException;
import java.sql.Types;

import static io.seata.sqlparser.util.JdbcConstants.MYSQL8;

/**
 * The ColumnMeta processor for mysql8.
 *
 * @author wang.liang
 */
@LoadLevel(name = MYSQL8, dependsOnClasses = {ResultSetMetaData.class, Field.class})
public class Mysql8ColumnMetaProcessor implements IColumnMetaProcessor {

    @Override
    public void process(ColumnMeta columnMeta, int columnIndex, ColumnMetaProcessorContext context) {
        this.readRealDataType(columnMeta, columnIndex, context);
    }


    /**
     * Read the realDataType for fix issue#6064
     *
     * @param columnMeta  the column meta
     * @param columnIndex the column index
     * @param context     the context
     * @see <a href="https://github.com/seata/seata/issues/6064">issue#6064</a>
     */
    protected void readRealDataType(ColumnMeta columnMeta, int columnIndex, ColumnMetaProcessorContext context) {
        if (columnMeta.getRealDataType() != null || columnMeta.getDataType() == Types.TINYINT) {
            return;
        }

        // unwrap ResultSetMetaData
        ResultSetMetaData rsmd;
        try {
            rsmd = context.getResultSetMetaData().unwrap(ResultSetMetaData.class);
        } catch (SQLException ignore) {
            return;
        }

        // get field
        Field field = rsmd.getFields()[columnIndex];

        if (field.getMysqlTypeId() == MysqlType.FIELD_TYPE_TINY) {
            columnMeta.setRealDataType(Types.TINYINT);
        }
    }
}
