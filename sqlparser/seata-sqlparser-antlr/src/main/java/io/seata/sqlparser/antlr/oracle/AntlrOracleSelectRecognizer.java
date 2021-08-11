package io.seata.sqlparser.antlr.oracle;

import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.antlr.oracle.listener.SelectSpecificationSqlListener;
import io.seata.sqlparser.antlr.oracle.parser.OracleLexer;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.stream.ANTLRNoCaseStringStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YechenGu
 */
public class AntlrOracleSelectRecognizer implements SQLSelectRecognizer {
    private OracleContext oracleContext;

    public AntlrOracleSelectRecognizer(String sql) {
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        context.setOriginalSQL(sql);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(context),scriptContext);
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.SELECT;
    }

    @Override
    public String getTableAlias() {
        return null;
    }

    @Override
    public String getTableName() {
        return null;
    }

    @Override
    public String getOriginalSQL() {
        return null;
    }

    @Override
    public String getWhereCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    public String getWhereCondition() {
        return null;
    }
}
