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

import java.util.Objects;

import javax.annotation.Resource;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.seata.server.storage.db.r2dbc.repository.DistributedLockRepository;
import io.seata.server.storage.db.r2dbc.entity.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.scheduler.Schedulers;

/**
 * @author jianbin.chen
 */
@ConditionalOnBean(DatabaseClient.class)
@Component
public class R2dbcDistributedLockerDAO implements DistributedLocker {
    private static final Logger LOGGER = LoggerFactory.getLogger(R2dbcDistributedLockerDAO.class);

    private BeanCopier distributedLockDOToEntity = BeanCopier.create(DistributedLockDO.class, DistributedLock.class, false);

    @Resource
    DistributedLockRepository distributedLockRepository;

    @Resource
    private TransactionalOperator operator;

    /**
     * Instantiates a new Log store data base dao.
     */
    public R2dbcDistributedLockerDAO() {}

    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        try {
            return Boolean.TRUE.equals(distributedLockRepository.findById(distributedLockDO.getLockKey())
                .publishOn(Schedulers.boundedElastic()).map(distributedLock -> {
                    if (distributedLock != null && StringUtils.isNotBlank(distributedLock.getLockValue())
                        && !StringUtils.equals(distributedLock.getLockValue(), distributedLockDO.getLockValue())
                        && System.currentTimeMillis() < distributedLock.getExpireTime()) {
                        return false;
                    }

                    if (distributedLock != null) {
                        distributedLock.setExpireTime(distributedLockDO.getExpireTime());
                        if (!StringUtils.equals(distributedLock.getLockValue(), distributedLockDO.getLockValue())) {
                            distributedLock.setLockValue(distributedLockDO.getLockValue());
                        }
                        distributedLock.setNewLock(false);
                        return distributedLockRepository.save(distributedLock).block() != null;
                    }
                    distributedLock = new DistributedLock();
                    distributedLockDOToEntity.copy(distributedLockDO, distributedLock, null);
                    return distributedLockRepository.save(distributedLock).block() != null;
                }).as(operator::transactional).block());
        } catch (R2dbcDataIntegrityViolationException e) {
            // being scrambled by other threads to succeed
            return false;
        }
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        try {
            return Boolean.TRUE.equals(distributedLockRepository.findById(distributedLockDO.getLockKey())
                .publishOn(Schedulers.boundedElastic()).map(distributedLock -> {
                    if (null == distributedLock) {
                        throw new ShouldNeverHappenException(
                            "distributedLockDO would not be null when release distribute lock");
                    }

                    if (distributedLock.getExpireTime() >= System.currentTimeMillis()
                        && !Objects.equals(distributedLock.getLockValue(), distributedLockDO.getLockValue())) {
                        LOGGER.debug("the distribute lock for key :{} is holding by :{}, skip the release lock.",
                            distributedLockDO.getLockKey(), distributedLock.getLockValue());
                        return true;
                    }
                    distributedLock.setNewLock(false);
                    distributedLock.setLockValue(StringUtils.SPACE);
                    distributedLock.setExpireTime(0L);
                    return distributedLockRepository.save(distributedLock).block() != null;
                }).as(operator::transactional).block());
        } catch (R2dbcDataIntegrityViolationException e) {
            // being scrambled by other threads to succeed
            return false;
        }
    }

}
