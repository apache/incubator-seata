package io.seata.rm.datasource.sql.struct;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.sql.struct.cache.MysqlTableMetaCache;
import io.seata.rm.datasource.sql.struct.cache.OracleTableMetaCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author guoyao
 * @date 2019-10-14
 */
public class TableMetaCacheFactoryTest {

    private static final String NOT_EXIST_SQL_TYPE = "not_exist_sql_type";

    @Test
    public void getTableMetaCache() {
        Assertions.assertTrue(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL) instanceof MysqlTableMetaCache);
        Assertions.assertTrue(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE) instanceof OracleTableMetaCache);
        Assertions.assertEquals(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE), TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE));
        Assertions.assertEquals(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL), TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            TableMetaCacheFactory.getTableMetaCache(NOT_EXIST_SQL_TYPE);
        });
    }
}
