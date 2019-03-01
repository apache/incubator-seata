package com.alibaba.fescar.rm.datasource.undo.mysql.keyword;

import com.alibaba.fescar.rm.datasource.undo.KeywordChecker;
import org.apache.commons.lang3.StringUtils;


/**
 * @author Wu
 * xingfudeshi@gmail.com
 * MySQL keyword checker
 */
public class MySQLKeywordChecker implements KeywordChecker {
    /**
     * MySQL keyword
     */
    private enum MySQLKeyword {
        SELECT("SELECT"),
        DELETE("DELETE"),
        INSERT("INSERT"),
        UPDATE("UPDATE"),

        FROM("FROM"),
        HAVING("HAVING"),
        WHERE("WHERE"),
        ORDER("ORDER"),
        BY("BY"),
        GROUP("GROUP"),
        INTO("INTO"),
        AS("AS"),

        CREATE("CREATE"),
        ALTER("ALTER"),
        DROP("DROP"),
        SET("SET"),

        NULL("NULL"),
        NOT("NOT"),
        DISTINCT("DISTINCT"),

        TABLE("TABLE"),
        TABLESPACE("TABLESPACE"),
        VIEW("VIEW"),
        SEQUENCE("SEQUENCE"),
        TRIGGER("TRIGGER"),
        USER("USER"),
        INDEX("INDEX"),
        SESSION("SESSION"),
        PROCEDURE("PROCEDURE"),
        FUNCTION("FUNCTION"),

        PRIMARY("PRIMARY"),
        KEY("KEY"),
        DEFAULT("DEFAULT"),
        CONSTRAINT("CONSTRAINT"),
        CHECK("CHECK"),
        UNIQUE("UNIQUE"),
        FOREIGN("FOREIGN"),
        REFERENCES("REFERENCES"),

        EXPLAIN("EXPLAIN"),
        FOR("FOR"),
        IF("IF"),
        SORT("SORT"),


        ALL("ALL"),
        UNION("UNION"),
        EXCEPT("EXCEPT"),
        INTERSECT("INTERSECT"),
        MINUS("MINUS"),
        INNER("INNER"),
        LEFT("LEFT"),
        RIGHT("RIGHT"),
        FULL("FULL"),
        OUTER("OUTER"),
        JOIN("JOIN"),
        ON("ON"),
        SCHEMA("SCHEMA"),
        CAST("CAST"),
        COLUMN("COLUMN"),
        USE("USE"),
        DATABASE("DATABASE"),
        TO("TO"),

        AND("AND"),
        OR("OR"),
        XOR("XOR"),
        CASE("CASE"),
        WHEN("WHEN"),
        THEN("THEN"),
        ELSE("ELSE"),
        ELSIF("ELSIF"),
        END("END"),
        EXISTS("EXISTS"),
        IN("IN"),
        CONTAINS("CONTAINS"),
        RLIKE("RLIKE"),
        FULLTEXT("FULLTEXT"),

        NEW("NEW"),
        ASC("ASC"),
        DESC("DESC"),
        IS("IS"),
        LIKE("LIKE"),
        ESCAPE("ESCAPE"),
        BETWEEN("BETWEEN"),
        VALUES("VALUES"),
        INTERVAL("INTERVAL"),

        LOCK("LOCK"),
        SOME("SOME"),
        ANY("ANY"),
        TRUNCATE("TRUNCATE"),

        RETURN("RETURN"),

        // mysql
        TRUE("TRUE"),
        FALSE("FALSE"),
        LIMIT("LIMIT"),
        KILL("KILL"),
        IDENTIFIED("IDENTIFIED"),
        PASSWORD("PASSWORD"),
        ALGORITHM("ALGORITHM"),
        DUAL("DUAL"),
        BINARY("BINARY"),
        SHOW("SHOW"),
        REPLACE("REPLACE"),


        // MySql procedure add by zz
        WHILE("WHILE"),
        DO("DO"),
        LEAVE("LEAVE"),
        ITERATE("ITERATE"),
        REPEAT("REPEAT"),
        UNTIL("UNTIL"),
        OPEN("OPEN"),
        CLOSE("CLOSE"),
        OUT("OUT"),
        INOUT("INOUT"),
        EXIT("EXIT"),
        UNDO("UNDO"),
        SQLSTATE("SQLSTATE"),
        CONDITION("CONDITION"),
        DIV("DIV");

        public final String name;

        MySQLKeyword() {
            this(null);
        }

        MySQLKeyword(String name) {
            this.name = name;
        }
    }


    @Override
    public boolean check(String fieldOrTableName) {
        try {
            if (StringUtils.isNotBlank(fieldOrTableName)) {
                MySQLKeyword.valueOf(fieldOrTableName.toUpperCase());
                return true;
            }
        } catch (IllegalArgumentException e) {
            //do nothing
        }
        return false;
    }

    @Override
    public String checkAndReplace(String fieldOrTableName) {
        try {
            if (StringUtils.isNotBlank(fieldOrTableName)) {
                MySQLKeyword.valueOf(fieldOrTableName.toUpperCase());
                return "`" + fieldOrTableName + "`";
            }
        } catch (IllegalArgumentException e) {
            //do nothing
        }
        return fieldOrTableName;
    }
}
