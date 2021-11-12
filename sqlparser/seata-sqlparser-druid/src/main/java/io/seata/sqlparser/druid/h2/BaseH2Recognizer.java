package io.seata.sqlparser.druid.h2;

import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.dialect.h2.visitor.H2OutputVisitor;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/**
 * @author hongyan
 * @date Created in 2021-11-10 23:29
 * @description
 */
public abstract class BaseH2Recognizer extends BaseRecognizer {

    /**
     * Instantiates a new h2 base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseH2Recognizer(String originalSql) {
        super(originalSql);
    }

    public H2OutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
                                               final ArrayList<List<Object>> paramAppenderList,
                                               final StringBuilder sb) {
        return new H2OutputVisitor(sb) {

            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
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

        executeVisit(where, new H2OutputVisitor(sb));
        return sb.toString();
    }

    protected String getLimitCondition(SQLLimit sqlLimit) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeLimit(sqlLimit, new H2OutputVisitor(sb));

        return sb.toString();
    }

    protected String getLimitCondition(SQLLimit sqlLimit, final ParametersHolder parametersHolder,
                                       final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeLimit(sqlLimit, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, new H2OutputVisitor(sb));

        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy, final ParametersHolder parametersHolder,
                                         final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }
}

