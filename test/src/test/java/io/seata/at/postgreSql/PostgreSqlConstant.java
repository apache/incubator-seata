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

    /**
     * number type
     */
    public static final int NUMBER_TYPE = 2;

    /**
     * char type
     */
    public static final int CHAR_TYPE = 3;

    public static final String CURRENCY_TABLE_NAME = "\"public\".\"CURRENCY_TEST\"";

    public static final String TEST_CURRENCY_TYPE_INSERT_SQL =
        "INSERT INTO " + CURRENCY_TABLE_NAME + "VALUES ('$10,000.00', 1)";

    public static final String TEST_CURRENCY_TYPE_UPDATE_SQL =
        "UPDATE" + CURRENCY_TABLE_NAME + "SET \"MONEY_TEST\" = '1231231231' WHERE \"ID\" =" + TEST_RECORD_ID;

    public static final String NUMBER_TABLE_NAME = "\"public\".\"NUMBER_TEST\"";

    public static final String TEST_NUMBER_TYPE_INSERT_SQL = "INSERT INTO " + NUMBER_TABLE_NAME
            + "( \"ID\", \"SMALLINT_TEST\", \"INTEGER_TEST\", \"BIGINT_TEST\", \"DECIMAL_TEST\", \"NUMERIC_TEST\", \"REAL_TEST\", \"DOUBLE_PRECISION_TEST\", \"SMALLSERIAL_TEST\", \"SERIAL_TEST\", \"BIGSERIAL_TEST\" )\n"
            + "VALUES\n"
            + "\t( 1, 112, 1231, 53235324234532, '123.123', '54353.1', '1.23', '12.12', 12, 123, 12312312313 );";

    public static final String TEST_NUMBER_TYPE_UPDATE_SQL = "UPDATE" + NUMBER_TABLE_NAME
            + "SET \"BIGSERIAL_TEST\" = 12312312312312312\n" + "WHERE \"ID\" = " + TEST_RECORD_ID;

    public static final String CHAR_TABLE_NAME = "\"public\".\"CHAR_TEST\"";

    public static final String TEST_CHAR_TYPE_INSERT_SQL = "INSERT INTO " + CHAR_TABLE_NAME
            + "( \"ID\", \"CHARACTER_TEST\", \"VARCHAR_TEST\", \"CHAR_TEST\", \"TEXT_TEST\" )\n" + "VALUES\n"
            + "\t( 1, '12   ', '12312', '1212                                                                                                            ', '131231EQWERQWERWQERQWR' );";

    public static final String TEST_CHAR_TYPE_UPDATE_SQL =
            "UPDATE" + CHAR_TABLE_NAME + "SET \"TEXT_TEST\" = '1231231QWERWQERWR'\n" + "WHERE \"ID\" = " + TEST_RECORD_ID;
}
