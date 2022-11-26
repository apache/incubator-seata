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
package io.seata.server.storage.db.r2dbc.lock;

import javax.annotation.Resource;

import io.seata.common.util.StringUtils;
import io.seata.server.storage.db.r2dbc.repository.DistributedLockRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;

/**
 * @author jianbin.chen
 */
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
@ConditionalOnBean(DatabaseClient.class)
@Component
public class R2dbcDistributedLockerDAO implements DistributedLocker {

    @Resource
    private DistributedLockRepository distributedLockRepository;

    /**
     * Instantiates a new Log store data base dao.
     */
    public R2dbcDistributedLockerDAO() {}

    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        try {
            distributedLockDO.setExpireTime(distributedLockDO.getExpireTime() + System.currentTimeMillis());
            Integer res = distributedLockRepository.acquireLock(distributedLockDO.getExpireTime(),
                distributedLockDO.getLockKey(), distributedLockDO.getLockValue(), System.currentTimeMillis()).block();
            return res != null && res > 0;
        } catch (R2dbcDataIntegrityViolationException e) {
            // being scrambled by other threads to succeed
            return false;
        }
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        try {
            Integer res = distributedLockRepository
                .releaseLock(distributedLockDO.getLockKey(), StringUtils.SPACE, distributedLockDO.getLockValue())
                .block();
            return res != null && res > 0;
        } catch (R2dbcDataIntegrityViolationException e) {
            // being scrambled by other threads to succeed
            return false;
        }
    }

}
