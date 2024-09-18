/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.seata.sqlparser.druid.oscar;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import org.apache.seata.sqlparser.util.JdbcConstants;

/**
 * The Type OscarOperateRecognizerHolder
 *
 * @author hsh
 */
@LoadLevel(name = JdbcConstants.OSCAR)
public class OscarOperateRecognizerHolder implements SQLOperateRecognizerHolder {

    @Override
    public SQLRecognizer getDeleteRecognizer(String sql, SQLStatement ast) {
        return new OscarDeleteRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getInsertRecognizer(String sql, SQLStatement ast) {
        return new OscarInsertRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getUpdateRecognizer(String sql, SQLStatement ast) {
        return new OscarUpdateRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getSelectForUpdateRecognizer(String sql, SQLStatement ast) {
        if (((SQLSelectStatement) ast).getSelect().getFirstQueryBlock().isForUpdate()) {
            return new OscarSelectForUpdateRecognizer(sql, ast);
        }
        return null;
    }
}
