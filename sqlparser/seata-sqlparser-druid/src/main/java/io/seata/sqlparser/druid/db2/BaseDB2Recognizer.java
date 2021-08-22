package io.seata.sqlparser.druid.db2;

/**
 * @author qingjiusanliangsan
 */


import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.dialect.db2.visitor.DB2OutputVisitor;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseDB2Recognizer extends BaseRecognizer{

    /**
     * Instantiates a new db2 base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseDB2Recognizer(String originalSql) {
        super(originalSql);
    }

    public DB2OutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
                                               final ArrayList<List<Object>> paramAppenderList, final StringBuilder sb) {
        DB2OutputVisitor visitor = new DB2OutputVisitor(sb) {

            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.size() == 0) {
                        oneParamValues.forEach(t -> paramAppenderList.add(new ArrayList<>()));
                    }
                    for (int i = 0; i < oneParamValues.size(); i++) {
                        Object o = oneParamValues.get(i);
                        paramAppenderList.get(i).add(o instanceof Null ? null : o);
                    }

                }
                return super.visit(x);
            }
        };
        return visitor;
    }

    public String getWhereCondition(SQLExpr where, final ParametersHolder parametersHolder,
                                    final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        executeVisit(where, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    public String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeVisit(where, new DB2OutputVisitor(sb));
        return sb.toString();
    }

}
