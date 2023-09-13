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

import io.seata.common.loader.LoadLevel;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The type Table meta cache.
 *
 * @author funkye
 */
@LoadLevel(name = JdbcConstants.MARIADB)
public class MariadbTableMetaCache extends MysqlTableMetaCache {

    @Override
    protected TableMeta fetchSchema(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT * FROM " + ColumnUtils.addEscape(tableName, JdbcConstants.MARIADB) + " LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return resultSetMetaToSchema(rs.getMetaData(), connection.getMetaData());
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } catch (Exception e) {
            throw new SQLException(String.format("Failed to fetch schema of %s", tableName), e);
        }
    }

}
