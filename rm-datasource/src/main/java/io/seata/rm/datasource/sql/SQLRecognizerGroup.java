package io.seata.rm.datasource.sql;

import com.alibaba.druid.sql.ast.SQLStatement;

/**
 * The interface SQLRecognizerGroup
 *
 * @author: Zhibei Hao丶
 * @date: 2019/11/8 17:39
 * @version: V1.0
 */
public interface SQLRecognizerGroup
{
  SQLRecognizer getDeleteRecognizer(String sql, SQLStatement ast);

  SQLRecognizer getInsertRecognizer(String sql, SQLStatement ast);

  SQLRecognizer getUpdateRecognizer(String sql, SQLStatement ast);

  SQLRecognizer getSelectForUpdateRecognizer(String sql, SQLStatement ast);

  String getDbType();

}
