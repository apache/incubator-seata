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
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link DruidSQLRecognizerFactoryImpl}
 *
 * @author ggndnn
 * @author hsien999
 */
public class DruidSQLRecognizerFactoryTest {
    @Test
    public void testSqlRecognizerCreation() {
        SQLRecognizerFactory recognizerFactory = EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);

        //test sql syntax
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql, JdbcConstants.MYSQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql, JdbcConstants.ORACLE)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql, JdbcConstants.POSTGRESQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql, JdbcConstants.OCEANBASE_ORACLE)));

        String sql2 = "update table a inner join table b on a.id = b.pid set a.name = ?";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.OCEANBASE_ORACLE));

        String sql3 = "update t set id = 1 where id in (select id from b)";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.OCEANBASE_ORACLE));

        String sql4 = "insert into a values (1, 2)";
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql4, JdbcConstants.MYSQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql4, JdbcConstants.ORACLE)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql4, JdbcConstants.POSTGRESQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql4, JdbcConstants.OCEANBASE_ORACLE)));

        String sql5 = "insert into a (id, name) values (1, 2), (3, 4)";
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql5, JdbcConstants.MYSQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql5, JdbcConstants.ORACLE)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql5, JdbcConstants.POSTGRESQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql5, JdbcConstants.OCEANBASE_ORACLE)));

        String sql6 = "insert into a select * from b";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.OCEANBASE_ORACLE));

        String sql7 = "delete from t where id = ?";
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql7, JdbcConstants.MYSQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql7, JdbcConstants.ORACLE)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql7, JdbcConstants.POSTGRESQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql7, JdbcConstants.OCEANBASE_ORACLE)));

        String sql8 = "delete from t where id in (select id from b)";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.OCEANBASE_ORACLE));

        String sql9 = "select * from t for update";
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql9, JdbcConstants.MYSQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql9, JdbcConstants.ORACLE)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql9, JdbcConstants.POSTGRESQL)));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql9, JdbcConstants.OCEANBASE_ORACLE)));

        String sql10 = "select * from (select * from t) for update";
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql10, JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql10, JdbcConstants.ORACLE));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql10, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql10, JdbcConstants.OCEANBASE_ORACLE));

        String sql11 = "insert all into t1 values(1) into t2 values(2)";
        Assertions.assertThrows(Exception.class, () -> recognizerFactory.create(sql11, JdbcConstants.MYSQL));
        Assertions.assertTrue(CollectionUtils.isEmpty(recognizerFactory.create(sql11, JdbcConstants.ORACLE)));
        Assertions.assertThrows(Exception.class, () -> recognizerFactory.create(sql11, JdbcConstants.POSTGRESQL));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql11, JdbcConstants.OCEANBASE_ORACLE)));
        Assertions.assertEquals(2, recognizerFactory.create(sql11, JdbcConstants.OCEANBASE_ORACLE).size());

        String sql12 = "insert all when col1 > 1 then into t1 values(1) when col2 > 1 then into t2 values(2) select col1, col2 from t3";
        Assertions.assertThrows(Exception.class, () -> recognizerFactory.create(sql12, JdbcConstants.MYSQL));
        Assertions.assertTrue(CollectionUtils.isEmpty(recognizerFactory.create(sql12, JdbcConstants.ORACLE)));
        Assertions.assertThrows(Exception.class, () -> recognizerFactory.create(sql12, JdbcConstants.POSTGRESQL));
        Assertions.assertFalse(CollectionUtils.isEmpty(recognizerFactory.create(sql12, JdbcConstants.OCEANBASE_ORACLE)));
        Assertions.assertEquals(2, recognizerFactory.create(sql12, JdbcConstants.OCEANBASE_ORACLE).size());
    }
}
