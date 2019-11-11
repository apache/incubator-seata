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
package io.seata.rm.datasource.undo;

import io.seata.common.loader.EnhancedServiceLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Type UndoExecutorGroupFactory
 *
 * @author: Zhibei Hao丶
 * @date: 2019/11/9 10:30
 */
public class UndoExecutorGroupFactory
{
  private static volatile Map<String, UndoExecutorGroup> executorGroupMap;

  /**
   * Get UndoExecutorGroup by db type
   *
   * @param dbType the db type
   * @return the UndoExecutorGroup
   */
  public static UndoExecutorGroup getUndoExecutorGroup(String dbType) {

    if (executorGroupMap == null) {
      synchronized (UndoExecutorGroupFactory.class) {
        if (executorGroupMap == null) {
          Map<String, UndoExecutorGroup> initializeMap = new HashMap<>();
          List<UndoExecutorGroup> groupList =
              EnhancedServiceLoader.loadAll(UndoExecutorGroup.class);
          for (UndoExecutorGroup group : groupList) {
            initializeMap.put(group.getDbType().toLowerCase(), group);
          }
          executorGroupMap = initializeMap;
        }
      }
    }
    if (executorGroupMap.containsKey(dbType)) {
      return executorGroupMap.get(dbType);
    }
    throw new UnsupportedOperationException(MessageFormat.format("now not support {0}", dbType));
  }
}
