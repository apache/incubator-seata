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

import com.mysql.jdbc.Field;
import com.mysql.jdbc.MysqlDefs;
import com.mysql.jdbc.ResultSetMetaData;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.ReflectionUtil;
import io.seata.sqlparser.struct.ColumnMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Types;

import static io.seata.sqlparser.util.JdbcConstants.MYSQL5;

/**
 * The ColumnMeta processor for mysql5.
 *
 * @author wang.liang
 */
@LoadLevel(name = MYSQL5, dependsOnClasses = {ResultSetMetaData.class, Field.class})
public class Mysql5ColumnMetaProcessor implements IColumnMetaProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mysql5ColumnMetaProcessor.class);

    /**
     * @see MysqlDefs#FIELD_TYPE_TINY
     */
    private static final int FIELD_TYPE_TINY = 1;


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
     * @see ResultSetMetaData#getField(int)
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

        Field field;
        try {
            // the columnIndex of the method 'ResultSetMetaData#getField(int columnIndex)' starts from 1, not 0
            columnIndex++;

            field = (Field) ReflectionUtil.invokeMethod(rsmd, "getField", new Class<?>[]{int.class}, new Object[]{columnIndex});
        } catch (NoSuchMethodException | InvocationTargetException e) {
            LOGGER.warn("Get field of the column '{}' failed, ignore the exception", columnMeta.getColumnName(), e);
            return;
        }
        if (field.getMysqlType() == FIELD_TYPE_TINY) {
            columnMeta.setRealDataType(Types.TINYINT);
        }
    }
}
