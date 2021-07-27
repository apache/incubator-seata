package io.seata.rm.datasource.sql.constant;

public class SqlConstants {
    /**
     * symbol
     */
    public static final String SPACE = " ";
    public static final String LEFT_PARENTHESES = "(";
    public static final String RIGHT_PARENTHESES = ")";
    public static final String DOT = ".";
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String APOSTROPHE = "'";
    public static final String DOUBLE_QUOTES = "\"";
    public static final String EQUAL = "=";
    public static final String UNDERSCORE = "_";
    public static final String ASTERISK = "*";
    public static final String PLACEHOLDER = "?";
    public static final String COLUMN_SEPARATOR = "|";

    /**
     * SQL key
     */
    public static final String SELECT = "SELECT";
    public static final String FROM = "FROM";
    public static final String WHERE = "WHERE";
    public static final String FOR_UPDATE = "FOR UPDATE";
    public static final String OR = "OR";
    public static final String AND = "AND";
    public static final String NULL = "NULL";
    public static final String PRIMARY = "PRIMARY";

    /**
     * Splicing SQL commonly used TEM
     */
    public static final String SELECT_TEM = SELECT + SPACE;
    public static final String SELECT_ALL = SELECT_TEM + ASTERISK + SPACE;
    public static final String SELECT_LAST_INSERT_ID = SELECT_TEM + "LAST_INSERT_ID()";
    public static final String FROM_TEM = SPACE + FROM + SPACE;
    public static final String WHERE_TEM = SPACE + WHERE + SPACE;
    public static final String WHERE_PREFIX = SPACE + WHERE + SPACE + LEFT_PARENTHESES;
    public static final String FOR_UPDATE_TEM = SPACE + FOR_UPDATE;
    public static final String OR_TEM = SPACE + OR + SPACE;
    public static final String OR_PREFIX = OR_TEM + LEFT_PARENTHESES;
    public static final String DEFAULT_PREFIX = " = DEFAULT(";
    public static final String PG_CURRVAL_PREFIX = SELECT_TEM + "currval(";
    public static final String ORACLE_CURRVAL_SUFFIX = ".currval FROM DUAL";
    public static final String LEFT_PARENTHESES_TEM = SPACE + LEFT_PARENTHESES + SPACE;
    public static final String RIGHT_PARENTHESES_TEM = SPACE + RIGHT_PARENTHESES + SPACE;
    public static final String RIGHT_PARENTHESES_SUFFIX = RIGHT_PARENTHESES + SPACE;
    public static final String COMMA_TEM = SPACE + COMMA + SPACE;
    public static final String JOINER_DELIMITER = COMMA + SPACE;
    public static final String ASTERISK_TEM = SPACE + ASTERISK + SPACE;
    public static final String EQUAL_TEM = SPACE + EQUAL + SPACE;
    public static final String AND_TEM = SPACE + AND + SPACE;
    public static final String IS_PLACEHOLDER = " is ? ";
    public static final String EQUAL_PLACEHOLDER = " = ? ";
    public static final String SHOW_VARIABLES_AUTO_INCREMENT = "SHOW VARIABLES LIKE 'auto_increment_increment'";

}
