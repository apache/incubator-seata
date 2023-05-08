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
package io.seata.sqlparser.druid.mariadb;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.druid.mysql.MySQLSelectForUpdateRecognizer;

/**
 * The type Mariadb select for update recognizer.
 *
 * @author funkye
 */
public class MariadbSelectForUpdateRecognizer extends MySQLSelectForUpdateRecognizer {

    /**
     * Instantiates a new Mariadb select for update recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public MariadbSelectForUpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL, ast);
    }
}
