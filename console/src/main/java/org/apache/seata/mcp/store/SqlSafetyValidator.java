/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.store;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class SqlSafetyValidator {

    private static final String UNDO_LOG_TABLE = "undo_log";

    public SQLSelectStatement validateMysqlSelect(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new StoreException("SQL cannot be empty");
        }
        List<SQLStatement> statements;
        try {
            statements = SQLUtils.parseStatements(sql, DbType.mysql);
        } catch (Exception e) {
            throw new StoreException("SQL parse failed");
        }
        if (statements.size() != 1) {
            throw new StoreException("Only a single SELECT statement is allowed");
        }
        SQLStatement statement = statements.get(0);
        if (!(statement instanceof SQLSelectStatement)) {
            throw new StoreException("Only SELECT statements are allowed");
        }
        SQLSelectStatement selectStatement = (SQLSelectStatement) statement;
        if (hasLockClause(selectStatement)) {
            throw new StoreException("SELECT locking clauses are not allowed");
        }
        if (containsUndoLog(selectStatement)) {
            throw new StoreException("Querying undo_log is not allowed");
        }
        return selectStatement;
    }

    public SQLSelectStatement validateMysqlSelect(String sql, String databaseName) {
        SQLSelectStatement selectStatement = validateMysqlSelect(sql);
        validateDatabaseScope(selectStatement, databaseName);
        return selectStatement;
    }

    private boolean hasLockClause(SQLSelectStatement statement) {
        final boolean[] result = new boolean[] {false};
        statement.accept(new SQLASTVisitorAdapter() {
            @Override
            public boolean visit(SQLSelectQueryBlock queryBlock) {
                if (queryBlock.isForUpdate() || queryBlock.isForShare()) {
                    result[0] = true;
                }
                return true;
            }
        });
        return result[0];
    }

    private boolean containsUndoLog(SQLSelectStatement statement) {
        SchemaStatVisitor visitor = SQLUtils.createSchemaStatVisitor(DbType.mysql);
        statement.accept(visitor);
        for (TableStat.Name name : visitor.getTables().keySet()) {
            String tableName = normalizeTableName(name.getName());
            if (UNDO_LOG_TABLE.equals(tableName)) {
                return true;
            }
        }
        return false;
    }

    private void validateDatabaseScope(SQLSelectStatement statement, String databaseName) {
        if (StringUtils.isBlank(databaseName)) {
            throw new StoreException("Database name cannot be empty");
        }
        SchemaStatVisitor visitor = SQLUtils.createSchemaStatVisitor(DbType.mysql);
        statement.accept(visitor);
        String normalizedDatabase = normalizeIdentifier(databaseName);
        for (TableStat.Name name : visitor.getTables().keySet()) {
            String tableName = name.getName();
            int index = tableName.lastIndexOf('.');
            if (index < 0) {
                continue;
            }
            String queriedDatabaseName = normalizeIdentifier(tableName.substring(0, index));
            if (!normalizedDatabase.equals(queriedDatabaseName)) {
                throw new StoreException("Cross-database query is not allowed");
            }
        }
    }

    private String normalizeTableName(String tableName) {
        String normalized = tableName;
        int index = normalized.lastIndexOf('.');
        if (index >= 0) {
            normalized = normalized.substring(index + 1);
        }
        return normalized.replace("`", "").replace("\"", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeIdentifier(String identifier) {
        return identifier.replace("`", "").replace("\"", "").toLowerCase(Locale.ROOT);
    }
}
