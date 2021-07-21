package io.seata.sqlparser.druid.sqlserver;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.sqlserver.ast.SQLServerSelectQueryBlock;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.sqlparser.SQLType;

import java.util.ArrayList;
import java.util.List;

/**
 * The type SqlServer select for update recognizer.
 *
 * @author GoodBoyCoder
 */
public class SqlServerSelectForUpdateRecognizer extends BaseSqlServerRecognizer implements SQLSelectRecognizer {

    private final SQLSelectStatement ast;

    /**
     * Instantiates a new sqlserver select for update recognizer.
     *
     * @param originalSql the original sql
     */
    public SqlServerSelectForUpdateRecognizer(String originalSql, SQLStatement ast) {
        super(originalSql);
        this.ast = (SQLSelectStatement) ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.SELECT_FOR_UPDATE;
    }

    private SQLServerSelectQueryBlock getSelect() {
        SQLSelect select = ast.getSelect();
        if (select == null) {
            throw new SQLParsingException("should never happen!");
        }
        SQLServerSelectQueryBlock selectQueryBlock = (SQLServerSelectQueryBlock) select.getQueryBlock();
        if (selectQueryBlock == null) {
            throw new SQLParsingException("should never happen!");
        }
        if (selectQueryBlock.getTop() != null) {
            //deal with top sql
            dealTop(ast);
        }
        return selectQueryBlock;
    }

    @Override
    public String getTableAlias() {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLTableSource tableSource = selectQueryBlock.getFrom();
        return tableSource.getAlias();
    }

    @Override
    public String getTableName() {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLTableSource tableSource = selectQueryBlock.getFrom();
        StringBuilder sb = new StringBuilder();
        SQLServerOutputVisitor visitor = new SQLServerOutputVisitor(sb) {
            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit((SQLExprTableSource) tableSource);
        return sb.toString();
    }

    @Override
    public String getWhereCondition(ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList) {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLExpr where = selectQueryBlock.getWhere();
        return super.getWhereCondition(where, parametersHolder, paramAppenderList);
    }

    @Override
    public String getWhereCondition() {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLExpr where = selectQueryBlock.getWhere();
        return super.getWhereCondition(where);
    }
}
