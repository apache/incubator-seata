package io.seata.sqlparser.druid.mysql;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import io.seata.sqlparser.SQLReplaceRecognizer;
import io.seata.sqlparser.SQLType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class MySQLReplaceRecognizer extends BaseMySQLRecognizer implements SQLReplaceRecognizer {

    private final SQLReplaceStatement ast;

    /**
     * Instantiates a new mysql base recognizer
     *
     * @param originalSql the original sql
     */
    public MySQLReplaceRecognizer(String originalSql, SQLStatement ast) {
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
        List<SQLExpr> columnSQLExprs = ast.getColumns();
        if (columnSQLExprs.isEmpty()) {
            // REPLACE INTO tbl_name SET col_name=value,
            return null;
        }
        List<String> list = new ArrayList<>(columnSQLExprs.size());
        for (SQLExpr expr : columnSQLExprs) {
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr)expr).getName());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public List<String> getReplaceValues() {
        List<SQLInsertStatement.ValuesClause> valuesList = ast.getValuesList();
        List<String> list = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause value: valuesList) {
            String values = value.toString().replace("VALUES", "").trim();
            // del ()
            if (values.length() > 1) {
                values = values.substring(1, values.length() - 1);
            }
            list.add(values);
        }
        return list;
    }

    @Override
    public String getSelectQuery() {
        String originSql = this.ast.getQuery().toString();
        if (originSql.equals("")) {
            return originSql;
        }
        originSql = originSql.trim().replace("\n\t", " ");
        return originSql;
    }

    @Override
    protected SQLStatement getAst() {
        return this.ast;
    }
}
