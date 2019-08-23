package io.seata.rm.datasource.undo;

import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UndoLogManagerFactoryTest {

  @Test
  void getDefaultInstance() {
    Assertions.assertTrue(
        UndoLogManagerFactory.getDefaultInstance() instanceof UndoLogManagerMySQL);
  }

  @Test
  void getInstance() {
    Assertions.assertTrue(
        UndoLogManagerFactory.getInstance(JdbcConstants.ORACLE) instanceof UndoLogManagerOracle);
    Assertions.assertTrue(
        UndoLogManagerFactory.getInstance(JdbcConstants.MYSQL) instanceof UndoLogManagerMySQL);
  }
}
