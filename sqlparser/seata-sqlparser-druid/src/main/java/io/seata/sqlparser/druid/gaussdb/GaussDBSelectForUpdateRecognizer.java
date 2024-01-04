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

package io.seata.sqlparser.druid.gaussdb;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.druid.postgresql.PostgresqlSelectForUpdateRecognizer;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type GaussDB select for update recognizer.
 *
 */
public class GaussDBSelectForUpdateRecognizer extends PostgresqlSelectForUpdateRecognizer {
    
    /**
     * Instantiates a new GaussDB select for update recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public GaussDBSelectForUpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL, ast);
    }

    @Override
    public String getDbType() {
        return JdbcConstants.GAUSSDB;
    }
}
