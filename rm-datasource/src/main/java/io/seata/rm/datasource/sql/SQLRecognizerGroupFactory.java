package io.seata.rm.datasource.sql;

import io.seata.common.loader.EnhancedServiceLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * The SQLRecognizerGroupFactory
 *
 * @author: Zhibei Hao丶
 * @date: 2019/11/8 17:37
 * @version: V1.0
 */
public class SQLRecognizerGroupFactory
{

  private static Object lockObj = new Object();
  private static Map<String, SQLRecognizerGroup> recognizerGroupMap;

  public static SQLRecognizerGroup getSQLRecognizerGroup(String dbType) {

    if (recognizerGroupMap == null) {
      synchronized (lockObj) {
        if (recognizerGroupMap == null) {
          recognizerGroupMap = new HashMap<>();
          List<SQLRecognizerGroup> groupList =
              EnhancedServiceLoader.loadAll(SQLRecognizerGroup.class);
          for (SQLRecognizerGroup group : groupList) {
            recognizerGroupMap.put(group.getDbType().toLowerCase(), group);
          }
        }
      }
    }
    if (recognizerGroupMap.containsKey(dbType)) {
      return recognizerGroupMap.get(dbType);
    }
    throw new RuntimeException(MessageFormat.format("now not support {0}", dbType));
  }
}
