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
package io.seata.server.storage.r2dbc.lock;

import static io.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import io.seata.server.storage.db.lock.LockStoreDataBaseDAO;
import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.model.LockStatus;
import io.seata.core.store.LockDO;
import io.seata.server.storage.r2dbc.entity.Lock;
import io.seata.server.storage.r2dbc.repository.LockRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * The type Data base lock store.
 *
 * @author funkye
 */
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
@Component
public class R2dbcLockStoreDataBaseDAO extends LockStoreDataBaseDAO {

    @Resource
    LockRepository lockRepository;

    @Resource
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Resource
    private TransactionalOperator operator;

    BeanCopier lockDOToEntity = BeanCopier.create(LockDO.class, Lock.class, false);

    /**
     * Instantiates a new Data base lock store dao.
     *
     */
    public R2dbcLockStoreDataBaseDAO() {
        super();
    }

    @Override
    public boolean acquireLock(LockDO lockDO) {
        return acquireLock(Collections.singletonList(lockDO));
    }

    @Override
    public boolean acquireLock(List<LockDO> lockDOs) {
        return acquireLock(lockDOs, true, false);
    }

    @Override
    public boolean acquireLock(List<LockDO> lockDOs, boolean autoCommit, boolean skipCheckLock) {
        try {
            String xid = lockDOs.get(0).getXid();
            Mono<List<Lock>> mono = skipCheckLock ? Mono.just(Collections.emptyList())
                : r2dbcEntityTemplate
                    .select(Query
                        .query(Criteria.where(ServerTableColumnsName.LOCK_TABLE_ROW_KEY)
                            .in(lockDOs.parallelStream().map(LockDO::getRowKey).collect(Collectors.toList()))
                            .and(Criteria.where(ServerTableColumnsName.LOCK_TABLE_XID).not(xid)))
                        .columns(ServerTableColumnsName.LOCK_TABLE_XID, ServerTableColumnsName.LOCK_TABLE_BRANCH_ID,
                            ServerTableColumnsName.LOCK_TABLE_PK, ServerTableColumnsName.LOCK_TABLE_TABLE_NAME,
                            ServerTableColumnsName.LOCK_TABLE_STATUS)
                        .sort(Sort.by(Sort.Order.desc(ServerTableColumnsName.LOCK_TABLE_STATUS))), Lock.class)
                    .collectList();
            return Boolean.TRUE.equals(mono.publishOn(Schedulers.boundedElastic()).map(list -> {
                if (CollectionUtils.isNotEmpty(list)) {
                    boolean failFast = false;
                    Lock lock = list.get(0);
                    LOGGER.info("Global lock on [{}:{}] is holding by xid {} branchId {}", lock.getTableName(),
                        lock.getPk(), lock.getXid(), lock.getBranchId());
                    if (lock.getStatus() == LockStatus.Rollbacking.getCode()) {
                        failFast = true;
                    }
                    if (failFast) {
                        throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
                    }
                    return false;
                }
                lockRepository.saveAll(lockDOs.parallelStream().map(lockDO -> {
                    Lock lock = new Lock();
                    lockDOToEntity.copy(lockDO, lock, null);
                    return lock;
                }).collect(Collectors.toList())).collectList().block();
                return true;
            }).as(operator::transactional).block());
        } catch (Exception e) {
            // lock fail
            if (e instanceof StoreException) {
                throw e;
            }
            throw new StoreException(e);
        }
    }

    @Override
    public boolean unLock(LockDO lockDO) {
        return unLock(Collections.singletonList(lockDO));
    }

    @Override
    public boolean unLock(List<LockDO> lockDOs) {
        Integer count =
            r2dbcEntityTemplate.delete(Lock.class).from(lockTable)
                .matching(Query.query(Criteria.where(ServerTableColumnsName.LOCK_TABLE_XID).is(lockDOs.get(0).getXid())
                    .and(Criteria.where(ServerTableColumnsName.LOCK_TABLE_ROW_KEY)
                        .in(lockDOs.parallelStream().map(LockDO::getRowKey).collect(Collectors.toList())))))
                .all().block();
        return count != null && count > 0;
    }

    @Override
    public boolean unLock(String xid) {
        r2dbcEntityTemplate.delete(Lock.class).from(lockTable)
            .matching(Query.query(Criteria.where(ServerTableColumnsName.LOCK_TABLE_XID).is(xid))).all().block();
        return true;
    }

    @Override
    public boolean unLock(Long branchId) {
        Integer count = r2dbcEntityTemplate.delete(Lock.class).from(lockTable)
            .matching(Query.query(Criteria.where(ServerTableColumnsName.LOCK_TABLE_BRANCH_ID).is(branchId))).all()
            .block();
        return count != null && count > 0;
    }

    @Override
    public boolean isLockable(List<LockDO> lockDOs) {
        String xid = lockDOs.get(0).getXid();
        List<Lock> list = r2dbcEntityTemplate
            .select(Query.query(Criteria.where(ServerTableColumnsName.LOCK_TABLE_XID).not(xid)
                .and(Criteria.where(ServerTableColumnsName.LOCK_TABLE_ROW_KEY)
                    .in(lockDOs.parallelStream().map(LockDO::getRowKey).collect(Collectors.toList())))),
                Lock.class)
            .collectList().block();
        return CollectionUtils.isEmpty(list);
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        r2dbcEntityTemplate.update(Lock.class).inTable(lockTable)
            .matching(Query.query(Criteria.where(ServerTableColumnsName.LOCK_TABLE_XID).is(xid)))
            .apply(Update.update(ServerTableColumnsName.LOCK_TABLE_STATUS, lockStatus.getCode())).block();
    }

}
