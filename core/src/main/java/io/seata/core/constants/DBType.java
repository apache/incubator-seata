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
package io.seata.core.constants;

import io.seata.common.util.StringUtils;

/**
 * database type
 *
 * @author zhangsen
 * @date 2019 /4/2
 */
public enum DBType {

    /**
     * Mysql db type.
     */
    MYSQL,

    /**
     * Oracle db type.
     */
    ORACLE,

    /**
     * Db 2 db type.
     */
    DB2,

    /**
     * Sqlserver db type.
     */
    SQLSERVER,

    /**
     * Sybaee db type.
     */
    SYBAEE,

    /**
     * H2 db type.
     */
    H2,

    /**
     * Sqlite db type.
     */
    SQLITE,

    /**
     * Access db type.
     */
    ACCESS,

    /**
     * Postgresql db type.
     */
    POSTGRESQL,

    /**
     * Oceanbase db type.
     */
    OCEANBASE;

    /**
     * Valueof db type.
     *
     * @param dbType the db type
     * @return the db type
     */
    public static DBType valueof(String dbType) {
        for (DBType dt : values()) {
            if (StringUtils.equalsIgnoreCase(dt.name(), dbType)) {
                return dt;
            }
        }
        throw new IllegalArgumentException("unknown dbtype:" + dbType);
    }

}
