--
--  Copyright 1999-2019 Seata.io Group.
--
--  Licensed under the Apache License, Version 2.0 (the "License");
--  you may not use this file except in compliance with the License.
--  You may obtain a copy of the License at
--
--       http://www.apache.org/licenses/LICENSE-2.0
--
--  Unless required by applicable law or agreed to in writing, software
--  distributed under the License is distributed on an "AS IS" BASIS,
--  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--  See the License for the specific language governing permissions and
--  limitations under the License.
--

-- String type
create table T_DATA_TYPE_STRING_TEST
(
    id             NUMBER,
    CHAR_TEST      CHAR(10)    default '',
    NCHAR_TEST     NCHAR(20)   default '',
    VARCHAR_TEST   VARCHAR(30) default '',
    VARCHAR2_TEST  VARCHAR2(40) default '',
    NVARCHAR2_TEST NVARCHAR2(50) default ''
);
ALTER TABLE T_DATA_TYPE_STRING_TEST
    ADD CONSTRAINT PK_T_DATA_TYPE_STRING_TEST PRIMARY KEY (id);

-- number
create table T_DATA_TYPE_NUMBER_TEST
(
    id             NUMBER,
    TINYINT_TEST   NUMBER(3,0),
    MEDUIMINT_TEST NUMBER(7,0),
    INT_TEST       NUMBER(10,0),
    BIGINT_TEST    NUMBER(20,0),
    SMALLINT_TEST  NUMBER(38,0),
    INTEGER_TEST   INTEGER(38,0),
    DECIMAL_TEST   DECIMAL(38, 0),
    NUMERIC_TEST   NUMERIC(38, 0),
    DEC_TEST       DEC(38, 0)
);
ALTER TABLE T_DATA_TYPE_NUMBER_TEST
    ADD CONSTRAINT PK_T_DATA_TYPE_NUMBER_TEST PRIMARY KEY (id);

-- date
create table T_DATA_TYPE_DATE_TEST
(
    id             NUMBER,
    DATE_TEST      date,
    TIMESTAMP_TEST timestamp(3),
    TIMESTAMP_TZ   timestamp(3) with time zone,
    TIMESTAMP_TZL  timestamp(3) with local time zone
);
ALTER TABLE T_DATA_TYPE_DATE_TEST
    ADD CONSTRAINT PK_T_DATA_TYPE_DATE_TEST PRIMARY KEY (id);

-- binary
create table T_DATA_TYPE_BINARY_TEST
(
    id         NUMBER,
    BLOB_TEST  BLOB,
    CLOB_TEST  CLOB,
    NCLOB_TEST NCLOB,
    BFILE_TEST BFILE
);
ALTER TABLE T_DATA_TYPE_BINARY_TEST
    ADD CONSTRAINT PK_T_DATA_TYPE_BINARY_TEST PRIMARY KEY (id);