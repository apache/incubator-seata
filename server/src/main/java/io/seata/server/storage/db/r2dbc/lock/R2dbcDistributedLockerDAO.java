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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * @author jianbin.chen
 */
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
@ConditionalOnBean(DatabaseClient.class)
@Component
public class R2dbcDistributedLockerDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(R2dbcDistributedLockerDAO.class);

    private final BeanCopier distributedLockDOToEntity = BeanCopier.create(DistributedLockDO.class, DistributedLock.class, false);

    @Resource
    private DistributedLockRepository distributedLockRepository;

    /**
     * Instantiates a new Log store data base dao.
     */
    public R2dbcDistributedLockerDAO() {
    }

    @Transactional
    public Mono<Boolean> acquireLock(DistributedLockDO distributedLockDO) {
        try {
            return distributedLockRepository.findByLockKey(distributedLockDO.getLockKey()).map(distributedLock -> {
                    if (distributedLock != null && StringUtils.isNotBlank(distributedLock.getLockValue())
                        && !StringUtils.equals(distributedLock.getLockValue(), distributedLockDO.getLockValue())
                        && System.currentTimeMillis() < distributedLock.getExpireTime()) {
                        return Mono.just(false);
                    }
                    distributedLockDO.setExpireTime(distributedLockDO.getExpireTime() + System.currentTimeMillis());
                    if (distributedLock != null) {
                        if (!StringUtils.equals(distributedLock.getLockValue(), distributedLockDO.getLockValue())) {
                            distributedLock.setLockValue(distributedLockDO.getLockValue());
                        }
                        distributedLock.setExpireTime(distributedLockDO.getExpireTime());
                        distributedLock.setNewLock(false);
                        return distributedLockRepository.save(distributedLock).then(Mono.just(true));
                    }
                    distributedLock = new DistributedLock();
                    distributedLockDOToEntity.copy(distributedLockDO, distributedLock, null);
                    return distributedLockRepository.save(distributedLock).then(Mono.just(true));
                }).flatMap(Mono::from);
        } catch (R2dbcDataIntegrityViolationException e) {
            // being scrambled by other threads to succeed
            return Mono.just(false);
        }
    }

    @Transactional
    public Mono<Boolean> releaseLock(DistributedLockDO distributedLockDO) {
        try {
            return distributedLockRepository.findByLockKey(distributedLockDO.getLockKey()).map(distributedLock -> {
                if (null == distributedLock) {
                    throw new ShouldNeverHappenException(
                        "distributedLockDO would not be null when release distribute lock");
                }
                if (distributedLock.getExpireTime() >= System.currentTimeMillis()
                    && !Objects.equals(distributedLock.getLockValue(), distributedLockDO.getLockValue())) {
                    LOGGER.debug("the distribute lock for key :{} is holding by :{}, skip the release lock.",
                        distributedLockDO.getLockKey(), distributedLock.getLockValue());
                    return Mono.just(true);
                }
                distributedLock.setNewLock(false);
                distributedLock.setLockValue(StringUtils.SPACE);
                distributedLock.setExpireTime(0L);
                return distributedLockRepository.save(distributedLock).then(Mono.just(true));
            }).flatMap(Mono::from);
        } catch (R2dbcDataIntegrityViolationException e) {
            // being scrambled by other threads to succeed
            return Mono.just(false);
        }
    }

}
