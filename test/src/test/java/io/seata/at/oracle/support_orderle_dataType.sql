-- String type
create table T_DATA_TYPE_STRING_TEST
(
    id             NUMBER,
    CHAR_TEST      CHAR(20)    default '',
    NCHAR_TEST     NCHAR(20)   default '',
    VARCHAR_TEST   VARCHAR(20) default '',
    VARCHAR2_TEST  VARCHAR2(20) default '',
    NVARCHAR2_TEST NVARCHAR2(20) default '',
    CLOB_TEST      CLOB        default ''
);

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
-- date
create table T_DATA_TYPE_DATE_TEST
(
    id             NUMBER,
    DATE_TEST      DATE,
    TIME_TEST      TIME,
    TIMESTAMP_TEST TIMESTAMP
);
-- binary
create table T_DATA_TYPE_BINARY_TEST
(
    id         NUMBER,
    BLOB_TEST  BLOB,
    CLOB_TEST  CLOB,
    NCLOB_TEST NCLOB,
    BFILE_TEST BFILE,
    RAW_TEST   RAW
);
--主键
ALTER TABLE T_DATA_TYPE_TEST
    ADD CONSTRAINT PK_T_DATA_TYPE_TEST PRIMARY KEY (ID);

create sequence SEQ_T_DATA_TYPE_TEST
    minvalue 1
    maxvalue 9999999999999999999
    start with 1
    increment by 1 cache 20;