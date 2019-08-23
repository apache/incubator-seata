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

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.loader.EnhancedServiceLoader;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UndoLogManager Factory
 *
 * @author: Zhibei Hao
 * @date: 2019/8/22 14:14
 */
public class UndoLogManagerFactory {
  private static final ConcurrentHashMap<String, AbstractUndoLogManager> INSTANCES =
      new ConcurrentHashMap<>();


  public static AbstractUndoLogManager getDefaultInstance() {
    return getInstance(JdbcConstants.MYSQL);
  }

  public static AbstractUndoLogManager getInstance(String dbType) {
    AbstractUndoLogManager result = INSTANCES.get(dbType);
    if (result == null) {
      synchronized (UndoLogManagerFactory.class) {
        result = INSTANCES.get(dbType);
        if (result == null) {
          List<AbstractUndoLogManager> managerList =
              EnhancedServiceLoader.loadAll(AbstractUndoLogManager.class);
          for (AbstractUndoLogManager manager : managerList) {
            INSTANCES.putIfAbsent(manager.getDbType().toLowerCase(), manager);
          }
          if (INSTANCES.containsKey(dbType)) {
            result = INSTANCES.get(dbType);
          } else {
            throw new RuntimeException(MessageFormat.format("now not support {0}", dbType));
          }
        }
      }
    }
    return result;
  }
}
