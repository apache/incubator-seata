package io.seata.sqlparser.druid.sqlserver;

import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLHint;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprHint;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Type SqlServerOperateRecognizerHolder
 *
 * @author GoodBoyCoder
 */
@LoadLevel(name = JdbcConstants.SQLSERVER)
public class SqlServerOperateRecognizerHolder implements SQLOperateRecognizerHolder {
    @Override
    public SQLRecognizer getDeleteRecognizer(String sql, SQLStatement ast) {
        return new SqlServerDeleteRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getInsertRecognizer(String sql, SQLStatement ast) {
        return new SqlServerInsertRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getUpdateRecognizer(String sql, SQLStatement ast) {
        return new SqlServerUpdateRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getSelectForUpdateRecognizer(String sql, SQLStatement ast) {
        List<SQLHint> hints = ((SQLSelectStatement) ast).getSelect().getQueryBlock().getFrom().getHints();
        if (CollectionUtils.isNotEmpty(hints)) {
            List<String> hintsTexts = hints
                    .stream()
                    .map(hint -> {
                        if (hint instanceof SQLExprHint) {
                            SQLExpr expr = ((SQLExprHint) hint).getExpr();
                            return expr instanceof SQLIdentifierExpr ? ((SQLIdentifierExpr) expr).getName() : "";
                        } else if (hint instanceof SQLCommentHint) {
                            return ((SQLCommentHint) hint).getText();
                        }
                        return "";
                    }).collect(Collectors.toList());
            if (hintsTexts.containsAll(Arrays.asList(SqlServerSelectForUpdateRecognizer.LOCK_1, SqlServerSelectForUpdateRecognizer.LOCK_2))) {
                return new SqlServerSelectForUpdateRecognizer(sql, ast);
            }
        }
        return null;
    }
}
