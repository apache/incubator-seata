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
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DruidSQLRecognizerFactoryTest {
    @Test
    public void testSqlRecognizerCreation() {
        SQLRecognizerFactory recognizerFactory = EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
        Assertions.assertNotNull(recognizerFactory);
        List<SQLRecognizer> recognizers = recognizerFactory.create("delete from t1", JdbcConstants.MYSQL);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(),1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        //test sql syntax
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.POSTGRESQL));

        String sql2 = "update table a inner join table b on a.id = b.pid set a.name = ?";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.POSTGRESQL));

        String sql3 = "update t set id = 1 where id in (select id from b)";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.POSTGRESQL));

        String sql4 = "insert into a values (1, 2)";
        Assertions.assertNotNull(recognizerFactory.create(sql4, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql4, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql4, JdbcConstants.POSTGRESQL));

        String sql5 = "insert into a (id, name) values (1, 2), (3, 4)";
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.POSTGRESQL));

        String sql6 = "insert into a select * from b";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.POSTGRESQL));

        String sql7 = "delete from t where id = ?";
        Assertions.assertNotNull(recognizerFactory.create(sql7, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql7, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql7, JdbcConstants.POSTGRESQL));

        String sql8 = "delete from t where id in (select id from b)";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.POSTGRESQL));

        String sql9 = "select * from t for update";
        Assertions.assertNotNull(recognizerFactory.create(sql9, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql9, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql9, JdbcConstants.POSTGRESQL));

        String sql10 = "select * from (select * from t) for update";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql10, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql10, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql10, JdbcConstants.POSTGRESQL));
    }
}
