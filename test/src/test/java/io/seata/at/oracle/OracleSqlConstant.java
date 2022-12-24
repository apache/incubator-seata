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
package io.seata.at.oracle;

public class OracleSqlConstant {

    public static final int TEST_RECORD_ID = 1;

    public static final int NUMBER_TYPE = 1;
    public static final int STRING_TYPE = 2;
    public static final int DATE_TYPE = 3;
    public static final int BINARY_TYPE = 4;

    /******************************** NUMBER **********************************************/
    public static final String NUMBER_TABLE_NAME = "T_DATA_TYPE_NUMBER_TEST";

    public static final String TEST_NUMBER_TYPE_INSERT_SQL = "INSERT INTO T_DATA_TYPE_NUMBER_TEST\n"
        + "    (id,TINYINT_TEST,MEDUIMINT_TEST,INT_TEST,BIGINT_TEST,SMALLINT_TEST,INTEGER_TEST,DECIMAL_TEST,NUMERIC_TEST,DEC_TEST)\n"
        + "    VALUES (1,123,456,55,234,56,22423,45645.22,897.333,999)";

    public static final String TEST_NUMBER_TYPE_UPDATE_SQL = "UPDATE T_DATA_TYPE_NUMBER_TEST\n"
        + "set TINYINT_TEST = 312,MEDUIMINT_TEST = 654,INT_TEST = 55,BIGINT_TEST = 432,\n"
        + "    SMALLINT_TEST = 65,INTEGER_TEST = 32422,DECIMAL_TEST = 54654.22,NUMERIC_TEST = 798.333,\n"
        + "    DEC_TEST = 888 WHERE id =" + TEST_RECORD_ID;
    /******************************** NUMBER **********************************************/

    /******************************** STRING **********************************************/
    public static final String STRING_TABLE_NAME = "T_DATA_TYPE_STRING_TEST";

    public static final String TEST_STRING_TYPE_INSERT_SQL =
        "INSERT INTO T_DATA_TYPE_STRING_TEST(id,CHAR_TEST,NCHAR_TEST,VARCHAR_TEST,VARCHAR2_TEST,NVARCHAR2_TEST)\n"
            + "    VALUES (1,'1231测试','678123123测试','623234测试','90eq9wer9测试','我测试')";

    public static final String TEST_STRING_TYPE_UPDATE_SQL = "UPDATE T_DATA_TYPE_STRING_TEST\n" + "    SET\n"
        + "        CHAR_TEST = '33333',NCHAR_TEST = '55555',VARCHAR_TEST = '测试汇总',VARCHAR2_TEST = '测试varchar2',\n"
        + "        NVARCHAR2_TEST = '最后一列' WHERE id = " + TEST_RECORD_ID;

    /******************************** STRING **********************************************/

    /******************************** DATE START **********************************************/
    public static final String DATE_TABLE_NAME = "T_DATA_TYPE_DATE_TEST";

    public static final String TEST_DATE_TYPE_INSERT_SQL =
        "INSERT INTO T_DATA_TYPE_DATE_TEST VALUES(1,SYSDATE,SYSDATE,SYSDATE,SYSDATE)";

    public static final String TEST_DATE_TYPE_UPDATE_SQL =
        "UPDATE T_DATA_TYPE_DATE_TEST SET DATE_TEST = SYSDATE+2,TIMESTAMP_TEST = SYSDATE+2,TIMESTAMP_TZ = SYSDATE+2,TIMESTAMP_TZL = SYSDATE+2 where ID = "
            + TEST_RECORD_ID;
    /******************************** DATE END **********************************************/

    /******************************** BINARY START **********************************************/
    public static final String BINARY_TABLE_NAME = "T_DATA_TYPE_BINARY_TEST";

    public static final String TEST_BINARY_TYPE_INSERT_SQL = "INSERT INTO T_DATA_TYPE_BINARY_TEST \n" + "VALUES(1,\n"
        + "TO_BLOB('111110000111111111111111111111111110000000010101001001010'),\n"
        + "TO_CLOB('01010101111110101111111111111111111111111111111111'),\n"
        + "TO_NCLOB('1111111111111111111000000000000000011110010101001'),\n"
        + "BFILENAME('BFILEDIR', 'seata/src/test/java/io/seata/at/oracle/bfile.txt')\n" + ")";

    public static final String TEST_BINARY_TYPE_UPDATE_SQL = "UPDATE T_DATA_TYPE_BINARY_TEST\n"
        + "SET BLOB_TEST = TO_BLOB('111111111000111111111111111'),\n"
        + "CLOB_TEST = TO_CLOB('111111111111000111111111111111'),\n"
        + "BFILE_TEST = BFILENAME('BFILEDIR', 'seata/src/test/java/io/seata/at/oracle/bfile.txt'),\n"
        + "NCLOB_TEST = TO_NCLOB('11111111111110000000111111111111111')\n" + "WHERE ID = " + TEST_RECORD_ID;

    /******************************** BINARY END **********************************************/
}
