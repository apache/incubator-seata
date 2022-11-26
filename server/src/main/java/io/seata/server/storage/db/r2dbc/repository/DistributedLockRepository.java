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
package io.seata.server.storage.db.r2dbc.repository;

import io.seata.server.storage.db.r2dbc.entity.DistributedLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

/**
 * @author funkye
 */
@ConditionalOnBean(DatabaseClient.class)
public interface DistributedLockRepository extends ReactiveCrudRepository<DistributedLock, String> {

    @Modifying
    @Query("UPDATE distributed_lock SET expire = :#{[0]},lock_value = :#{[2]}"
        + " where lock_key = :#{[1]} and (lock_value = :#{[2]} or expire=0 or expire < :#{[3]})")
    Mono<Integer> acquireLock(Long expireTime, String lockKey, String lockValue, Long oldExpireTime);

    @Modifying
    @Query("UPDATE distributed_lock SET expire = 0,lock_value = :#{[1]}"
        + " where lock_key = :#{[0]} and lock_value = :#{[2]}")
    Mono<Integer> releaseLock(String lockKey, String lockValue, String oldLockValue);

}
