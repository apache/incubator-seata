package io.seata.rm.datasource.undo;

import io.seata.common.loader.EnhancedServiceLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 功能描述:
 *
 * @author: Zhibei Hao丶
 * @date: 2019/11/9 10:30
 * @version: V1.0
 */
public class UndoExecutorGroupFactory
{
  private static Map<String, UndoExecutorGroup> executorGroupMap;

  public static UndoExecutorGroup getUndoExecutorGroup(String dbType) {

    if (executorGroupMap == null) {
      synchronized (UndoExecutorGroupFactory.class) {
        if (executorGroupMap == null) {
          executorGroupMap = new HashMap<>();
          List<UndoExecutorGroup> groupList =
              EnhancedServiceLoader.loadAll(UndoExecutorGroup.class);
          for (UndoExecutorGroup group : groupList) {
            executorGroupMap.put(group.getDbType().toLowerCase(), group);
          }
        }
      }
    }
    if (executorGroupMap.containsKey(dbType)) {
      return executorGroupMap.get(dbType);
    }
    throw new RuntimeException(MessageFormat.format("now not support {0}", dbType));
  }
}
