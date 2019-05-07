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
package io.seata.rm.datasource.sql.druid;

import java.util.Arrays;
import java.util.Collections;

import com.alibaba.druid.sql.ast.SQLStatement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type My sql insert recognizer test.
 *
 * @author hanwen created at 2019-01-25
 */
public class MySQLInsertRecognizerTest extends AbstractMySQLRecognizerTest {

    /**
     * Insert recognizer test 0.
     */
    @Test
    public void insertRecognizerTest_0() {

        String sql = "INSERT INTO t1 (name) VALUES ('name1')";

        SQLStatement statement = getSQLStatement(sql);

        MySQLInsertRecognizer mySQLInsertRecognizer = new MySQLInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLInsertRecognizer.getTableName());
        Assertions.assertEquals(Collections.singletonList("name"), mySQLInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, mySQLInsertRecognizer.getInsertRows().size());
        Assertions.assertEquals(Collections.singletonList("name1"), mySQLInsertRecognizer.getInsertRows().get(0));
    }

    /**
     * Insert recognizer test 1.
     */
    @Test
    public void insertRecognizerTest_1() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 'name2')";

        SQLStatement statement = getSQLStatement(sql);

        MySQLInsertRecognizer mySQLInsertRecognizer = new MySQLInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLInsertRecognizer.getTableName());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, mySQLInsertRecognizer.getInsertRows().size());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertRows().get(0));
    }

    /**
     * Insert recognizer test 3.
     */
    @Test
    public void insertRecognizerTest_3() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 'name2'), ('name3', 'name4'), ('name5', 'name6')";

        SQLStatement statement = getSQLStatement(sql);

        MySQLInsertRecognizer mySQLInsertRecognizer = new MySQLInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLInsertRecognizer.getTableName());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(3, mySQLInsertRecognizer.getInsertRows().size());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertRows().get(0));
        Assertions.assertEquals(Arrays.asList("name3", "name4"), mySQLInsertRecognizer.getInsertRows().get(1));
        Assertions.assertEquals(Arrays.asList("name5", "name6"), mySQLInsertRecognizer.getInsertRows().get(2));
    }

}
