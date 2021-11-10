package io.seata.sqlparser.druid.h2;

/**
 * @author hongyan
 * @version 1.0
 * @date Created in 2021-11-09 17:03
 * @description
 */

import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.h2.visitor.H2OutputVisitor;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class BaseH2Recognizer extends BaseRecognizer {
    public BaseH2Recognizer(String originalSql) {
        super(originalSql);
    }

    public H2OutputVisitor createOutputVisitor(final ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList, StringBuilder sb) {
        return new H2OutputVisitor(sb) {
            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = (ArrayList)parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
                        oneParamValues.forEach((t) -> {
                            paramAppenderList.add(new ArrayList());
                        });
                    }

                    for(int i = 0; i < oneParamValues.size(); ++i) {
                        Object o = oneParamValues.get(i);
                        ((List)paramAppenderList.get(i)).add(o instanceof Null ? null : o);
                    }
                }

                return super.visit(x);
            }
        };
    }

    public String getWhereCondition(SQLExpr where, ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            this.executeVisit(where, this.createOutputVisitor(parametersHolder, paramAppenderList, sb));
            return sb.toString();
        }
    }

    public String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            this.executeVisit(where, new H2OutputVisitor(sb));
            return sb.toString();
        }
    }

    protected String getLimit(SQLStatement sqlStatement, SQLType sqlType, ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLLimit limit = null;
        if (SQLType.UPDATE == sqlType) {
//            limit = ((SQLUpdateStatement)sqlStatement).getLimit();
        } else if (SQLType.DELETE == sqlType) {
//            limit = ((SQLDeleteStatement)sqlStatement).getLimit();
        }

        if (limit != null) {
            StringBuilder builder = new StringBuilder(" LIMIT ");
            SQLIntegerExpr expr;
            Map parameters;
            if (limit.getOffset() != null) {
                if (limit.getOffset() instanceof SQLVariantRefExpr) {
                    builder.append("?,");
                    parameters = parametersHolder.getParameters();
                    paramAppenderList.add((List<Object>) parameters.get(parameters.size() - 1));
                } else {
                    expr = (SQLIntegerExpr)limit.getOffset();
                    builder.append(expr.getNumber()).append(",");
                }
            }

            if (limit.getRowCount() != null) {
                if (limit.getRowCount() instanceof SQLVariantRefExpr) {
                    builder.append("?");
                    parameters = parametersHolder.getParameters();
                    paramAppenderList.add((List<Object>) parameters.get(parameters.size()));
                } else {
                    expr = (SQLIntegerExpr)limit.getRowCount();
                    builder.append(expr.getNumber());
                }
            }

            return builder.toString();
        } else {
            return null;
        }
    }

    protected String getOrderBy(SQLStatement sqlStatement, SQLType sqlType) {
        SQLOrderBy orderBy = null;
        if (SQLType.UPDATE == sqlType) {
            orderBy = ((SQLUpdateStatement)sqlStatement).getOrderBy();
        } else if (SQLType.DELETE == sqlType) {
//            orderBy = ((SQLDeleteStatement)sqlStatement).getOrderBy();
        }

        if (orderBy != null) {
            String space = " ";
            String comma = ",";
            StringBuilder builder = (new StringBuilder(space)).append("ORDER BY").append(space);
            List<SQLSelectOrderByItem> items = orderBy.getItems();

            for(int i = 0; i < items.size(); ++i) {
                SQLSelectOrderByItem item = (SQLSelectOrderByItem)items.get(i);
                SQLIdentifierExpr expr = (SQLIdentifierExpr)item.getExpr();
                builder.append(expr.getName());
                SQLOrderingSpecification specification = item.getType();
                if (specification != null) {
                    builder.append(space);
                    builder.append(specification.name);
                }

                if (i + 1 != items.size()) {
                    builder.append(comma);
                }
            }

            return builder.toString();
        } else {
            return null;
        }
    }
}
