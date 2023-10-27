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
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.MARIADB);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.POLARDBX);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        //test sql syntax
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.POSTGRESQL));

        String sql1 = "update a set a.id = (select id from b where a.pid = b.pid)";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.ORACLE));
        String sql2 = "update (select a.id,a.name from a inner join b on a.id = b.id) t set t.name = 'xxx'";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.ORACLE));
        String sql3 = "update a set id = b.pid from b where a.id = b.id";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.POSTGRESQL));

        String sql4 = "update t set id = 1 where id in (select id from b)";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql4, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql4, JdbcConstants.MARIADB));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql4, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql4, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql4, JdbcConstants.POSTGRESQL));

        String sql5 = "insert into a values (1, 2)";
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.POSTGRESQL));

        String sql6 = "insert into a (id, name) values (1, 2), (3, 4)";
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.POSTGRESQL));

        String sql7 = "insert into a select * from b";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql7, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql7, JdbcConstants.MARIADB));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql7, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql7, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql7, JdbcConstants.POSTGRESQL));

        String sql8 = "delete from t where id = ?";
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.POSTGRESQL));

        String sql9 = "delete from t where id in (select id from b)";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql9, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql9, JdbcConstants.MARIADB));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql9, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql9, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql9, JdbcConstants.POSTGRESQL));

        String sql10 = "select * from t for update";
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.POSTGRESQL));

        String sql11 = "select * from (select * from t) for update";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql11, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql11, JdbcConstants.MARIADB));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql11, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql11, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql11, JdbcConstants.POSTGRESQL));
    }
}
