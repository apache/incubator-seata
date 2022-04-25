package io.seata.sqlparser.druid.mysql;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlRepeatStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLReplaceIntoRecognizer;
import io.seata.sqlparser.SQLType;

import java.util.List;

public class MySQLReplaceIntoRecognizer extends BaseMySQLRecognizer implements SQLReplaceIntoRecognizer {

    private final SQLReplaceStatement ast;

    /**
     * Instantiates a new mysql base recognizer
     *
     * @param originalSql the original sql
     */
    public MySQLReplaceIntoRecognizer(String originalSql, SQLStatement ast) {
        super(originalSql);
        this.ast = (SQLReplaceStatement) ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.REPLACE;
    }

    @Override
    public String getTableAlias() {
        return ast.getTableSource().getAlias();
    }

    @Override
    public String getTableName() {
        return ast.getTableName().getSimpleName();
    }

    @Override
    public boolean selectQueryIsEmpty() {
        if (this.ast.getQuery() == null) {
            return true;
        }
        return false;
    }

    @Override
    public List<String> getReplaceColumns() {
        return null;
    }

    @Override
    public List<String> getReplaceValues() {
        return this.ast.
    }

    @Override
    public String getSelectQuery() {
        return this.ast.getQuery().toString();
    }

    @Override
    protected SQLStatement getAst() {
        return this.ast;
    }
}
