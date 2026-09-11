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
package io.seata.sqlparser.util;

import java.util.ArrayList;
import java.util.List;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.EscapeHandler;
import io.seata.sqlparser.EscapeHandlerFactory;
import io.seata.sqlparser.struct.TableMeta;

/**
 * column utils
 *
 * @author jsbxyyx
 */
public final class ColumnUtils {

    /**
     * del escape by db type
     *
     * @param cols   the cols
     * @param dbType the db type
     * @return list
     */
    public static List<String> delEscape(List<String> cols, String dbType) {
        if (CollectionUtils.isEmpty(cols)) {
            return cols;
        }
        List<String> newCols = new ArrayList<>(cols.size());
        for (int i = 0, len = cols.size(); i < len; i++) {
            String col = cols.get(i);
            col = delEscape(col, dbType);
            newCols.add(col);
        }
        return newCols;
    }


    /**
     * del escape by escape
     *
     * @param colName the column name
     * @param dbType  the dbType
     * @return string string
     */
    public static String delEscape(String colName, String dbType) {
        EscapeHandler escapeHandler = EscapeHandlerFactory.getEscapeHandler(dbType);
        return escapeHandler.delColNameEscape(colName);
    }

    /**
     * if necessary, add escape by db type
     * <pre>
     * mysql:
     *   only deal with keyword.
     * postgresql:
     *   only deal with keyword, contains uppercase character.
     * oracle:
     *   only deal with keyword, not full uppercase character.
     * </pre>
     *
     * @param cols   the column name list
     * @param dbType the db type
     * @return list list
     */
    public static List<String> addEscape(List<String> cols, String dbType) {
        if (CollectionUtils.isEmpty(cols)) {
            return cols;
        }
        List<String> newCols = new ArrayList<>(cols.size());
        for (int i = 0, len = cols.size(); i < len; i++) {
            String col = cols.get(i);
            col = addEscape(col, dbType);
            newCols.add(col);
        }
        return newCols;
    }

    /**
     * if necessary, add escape
     *
     * @param colName the column name
     * @param dbType  the dbType
     * @return colName
     */
    public static String addEscape(String colName, String dbType) {
        return addEscape(colName, dbType, null);
    }

    /**
     * if necessary, add escape
     *
     * @param colName the column name
     * @param dbType  the dbType
     * @param tableMeta  the tableMeta
     * @return colName
     */
    public static String addEscape(String colName, String dbType, TableMeta tableMeta) {
        if (StringUtils.isBlank(colName)) {
            return colName;
        }
        EscapeHandler escapeHandler = EscapeHandlerFactory.getEscapeHandler(dbType);
        return escapeHandler.addColNameEscape(colName, tableMeta);
    }

}
