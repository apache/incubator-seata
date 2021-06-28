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
package io.seata.sqlparser.antlr.mysql;

import io.seata.common.loader.LoadLevel;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.antlr.SQLOperateRecognizerHolder;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The class MySqlOperateRecognizerHolder
 *
 * @author zhihou
 */
@LoadLevel(name = JdbcConstants.MYSQL)
public class MySQLOperateRecognizerHolder implements SQLOperateRecognizerHolder {

    @Override
    public SQLRecognizer getDeleteRecognizer(String sql) {
        return new AntlrMySQLDeleteRecognizer(sql);
    }

    @Override
    public SQLRecognizer getInsertRecognizer(String sql) {
        return new AntlrMySQLInsertRecognizer(sql);
    }

    @Override
    public SQLRecognizer getUpdateRecognizer(String sql) {
        return new AntlrMySQLUpdateRecognizer(sql);
    }

    @Override
    public SQLRecognizer getSelectForUpdateRecognizer(String sql) {
        return new AntlrMySQLSelectRecognizer(sql);
    }
}
