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
/**
 * author doubleDimple
 */
package io.seata.at.postgreSql;

import static io.seata.at.oracle.OracleSqlConstant.TEST_RECORD_ID;

public class PostgreSqlConstant {

    /**
     * currency type
     */
    public static final int CURRENCY_TYPE = 1;

    public static final String CURRENCY_TABLE_NAME = "\"public\".\"CURRENCY_TEST\"";

    public static final String TEST_CURRENCY_TYPE_INSERT_SQL =
        "INSERT INTO " + CURRENCY_TABLE_NAME + "VALUES ('$10,000.00', 1)";

    public static final String TEST_CURRENCY_TYPE_UPDATE_SQL =
        "UPDATE" + CURRENCY_TABLE_NAME + "SET \"MONEY_TEST\" = '1231231231' WHERE \"ID\" =" + TEST_RECORD_ID;
}
