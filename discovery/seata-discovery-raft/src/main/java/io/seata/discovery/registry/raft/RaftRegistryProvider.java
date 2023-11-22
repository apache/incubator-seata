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
<<<<<<<< HEAD:core/src/main/java/io/seata/core/store/db/sql/lock/DmLockStoreSql.java
package io.seata.core.store.db.sql.lock;

import io.seata.common.loader.LoadLevel;

/**
 * the database lock store DaMeng sql
 *
 * @author wang.liang
 * @since 1.8.0
 */
@LoadLevel(name = "dm")
public class DmLockStoreSql extends MysqlLockStoreSql {
========
package io.seata.discovery.registry.raft;

import io.seata.common.loader.LoadLevel;
import io.seata.discovery.registry.RegistryProvider;
import io.seata.discovery.registry.RegistryService;

/**
 * @author funkye
 */
@LoadLevel(name = "Raft", order = 1)
public class RaftRegistryProvider implements RegistryProvider {

    @Override
    public RegistryService<?> provide() {
        return RaftRegistryServiceImpl.getInstance();
    }
>>>>>>>> upstream/2.x:discovery/seata-discovery-raft/src/main/java/io/seata/discovery/registry/raft/RaftRegistryProvider.java
}
