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
package io.seata.server.console.impl.file;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.console.result.SingleResult;
import io.seata.server.console.impl.AbstractLockService;
import io.seata.server.console.param.GlobalLockParam;
import io.seata.console.result.PageResult;
import io.seata.server.console.vo.GlobalLockVO;
import io.seata.core.lock.RowLock;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;

import io.seata.server.storage.file.lock.FileLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static io.seata.common.util.StringUtils.isBlank;
import static io.seata.server.console.vo.GlobalLockVO.convert;
import static java.util.Objects.isNull;

/**
 * Global Lock File ServiceImpl
 *
 * @author zhongxiang.wang
 * @author miaoxueyu
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'file'.equals('${lockMode}')}")
public class GlobalLockFileServiceImpl extends AbstractLockService implements GlobalLockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLockFileServiceImpl.class);

    @Override
    public PageResult<GlobalLockVO> query(GlobalLockParam param) {
        checkParam(param);

        final Collection<GlobalSession> allSessions = SessionHolder.getRootSessionManager().allSessions();

        final AtomicInteger total = new AtomicInteger();
        List<RowLock> result = allSessions
                .parallelStream()
                .filter(obtainGlobalSessionPredicate(param))
                .flatMap(globalSession -> globalSession.getBranchSessions().stream())
                .filter(obtainBranchSessionPredicate(param))
                .flatMap(branchSession -> filterAndMap(param, branchSession))
                .peek(globalSession -> total.incrementAndGet())
                .collect(Collectors.toList());

        return PageResult.build(convert(result), param.getPageNum(), param.getPageSize());

    }

    @Override
    public SingleResult<Void> deleteLock(GlobalLockParam param) {
        CheckResult checkResult = checkDeleteParam(param);
        BranchSession branchSession = checkResult.getBranchSession();
        ConcurrentMap<String, ConcurrentMap<String,
                ConcurrentMap<Integer, FileLocker.BucketLockMap>>> lockMap = FileLocker.getLockMap();
        Optional.ofNullable(lockMap.get(param.getResourceId()))
                .map(dbLockMap -> dbLockMap.get(param.getTableName()))
                .map(tableLockMap -> {
                    int bucketId = param.getPk().hashCode() % FileLocker.getBucketPerTable();
                    return tableLockMap.get(bucketId);
                })
                .ifPresent(bucketLockMap -> {
                    Map<FileLocker.BucketLockMap, Set<String>> lockHolder = branchSession.getLockHolder();
                    for (Map.Entry<FileLocker.BucketLockMap, Set<String>> entry : lockHolder.entrySet()) {
                        FileLocker.BucketLockMap key = entry.getKey();
                        // delete lock only if the same bucket in the branch
                        if (key.equals(bucketLockMap)) {
                            doDeleteGlobalLock(branchSession, param, entry);
                            break;
                        }
                    }
                });
        return SingleResult.success();
    }

    /**
     * filter with tableName and generate RowLock
     *
     * @param param         the query param
     * @param branchSession the branch session
     * @return the RowLock list
     */
    private Stream<RowLock> filterAndMap(GlobalLockParam param, BranchSession branchSession) {
        if (CollectionUtils.isEmpty(branchSession.getLockHolder())) {
            return Stream.empty();
        }

        final String tableName = param.getTableName();

        // get rowLock from branchSession
        final List<RowLock> rowLocks = LockerManagerFactory.getLockManager().collectRowLocks(branchSession);

        if (StringUtils.isNotBlank(tableName)) {
            return rowLocks.parallelStream().filter(rowLock -> rowLock.getTableName().contains(param.getTableName()));
        }

        return rowLocks.stream();
    }


    /**
     * check the param
     *
     * @param param the param
     */
    private void checkParam(GlobalLockParam param) {
        if (param.getPageSize() <= 0 || param.getPageNum() <= 0) {
            throw new IllegalArgumentException("wrong pageSize or pageNum");
        }

        // verification data type
        try {
            Long.parseLong(param.getTransactionId());
        } catch (NumberFormatException e) {
            param.setTransactionId(null);
        }
        try {
            Long.parseLong(param.getBranchId());
        } catch (NumberFormatException e) {
            param.setBranchId(null);
        }


    }

    /**
     * obtain the branch session condition
     *
     * @param param condition for query branch session
     * @return the filter condition
     */
    private Predicate<? super BranchSession> obtainBranchSessionPredicate(GlobalLockParam param) {
        return branchSession -> {
            // transactionId
            return (isBlank(param.getTransactionId()) ||
                    String.valueOf(branchSession.getTransactionId()).contains(param.getTransactionId()))

                    &&
                    // branch id
                    (isBlank(param.getBranchId()) ||
                            String.valueOf(branchSession.getBranchId()).contains(param.getBranchId()))
                    ;
        };
    }


    /**
     * obtain the global session condition
     *
     * @param param condition for query global session
     * @return the filter condition
     */
    private Predicate<? super GlobalSession> obtainGlobalSessionPredicate(GlobalLockParam param) {

        return globalSession -> {
            // first, there must be withBranchSession
            return CollectionUtils.isNotEmpty(globalSession.getBranchSessions())

                    &&
                    // The second is other conditions
                    // xid
                    (isBlank(param.getXid()) || globalSession.getXid().contains(param.getXid()))

                    &&
                    // timeStart
                    (isNull(param.getTimeStart()) || param.getTimeStart() <= globalSession.getBeginTime())

                    &&
                    // timeEnd
                    (isNull(param.getTimeEnd()) || param.getTimeEnd() >= globalSession.getBeginTime());
        };
    }

    private void doDeleteGlobalLock(BranchSession branchSession, GlobalLockParam param,
                                    Map.Entry<FileLocker.BucketLockMap, Set<String>> entry) {
        Map<FileLocker.BucketLockMap, Set<String>> lockHolder = branchSession.getLockHolder();
        String delPk = param.getPk();
        String delTableName = param.getTableName();
        FileLocker.BucketLockMap bucket = entry.getKey();
        Set<String> lockedPks = entry.getValue();
        for (String lockedPk : lockedPks) {
            // delete the lock when the pk is the same
            if (StringUtils.isNotBlank(lockedPk) && lockedPk.equalsIgnoreCase(delPk)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("start to delete global lock: xid{}, branchId:{}, tableName:{} pk:{}",
                            param.getXid(), param.getBranchId(), delTableName, delPk);
                }
                // remove the lock
                bucket.get().remove(lockedPk, branchSession);
                String[] tableGroupedLockKeys = branchSession.getLockKey().split(";");
                StringJoiner newLockKey = new StringJoiner(";");
                for (String tableGroupedLockKey : tableGroupedLockKeys) {
                    int idx = tableGroupedLockKey.indexOf(":");
                    String tableName = tableGroupedLockKey.substring(0, idx);
                    if (!tableName.equalsIgnoreCase(delTableName)) {
                        newLockKey.add(tableGroupedLockKey);
                        continue;
                    }
                    String mergedPKs = tableGroupedLockKey.substring(idx + 1);
                    String[] pks = mergedPKs.split(",");
                    StringJoiner newTablePkKey = new StringJoiner(",");
                    for (String pk : pks) {
                        // the pk that still contained, just append to the new lock key
                        if (!pk.equalsIgnoreCase(delPk)) {
                            newTablePkKey.add(pk);
                        }
                    }
                    if (StringUtils.isNotBlank(newTablePkKey.toString())) {
                        newLockKey.add(tableName + ":" + newTablePkKey);
                    }
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("update lock key from:{} to:{}", branchSession.getLockKey(), newLockKey);
                }
                // update the lock key in branchSession
                branchSession.setLockKey(newLockKey.toString());
                break;
            }
        }
        // remove the empty bucket
        if (bucket.get().isEmpty()) {
            lockHolder.remove(bucket);
        }
    }

    private CheckResult checkDeleteParam(GlobalLockParam param) {
        String xid = param.getXid();
        String branchId = param.getBranchId();
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        if (StringUtils.isBlank(param.getTableName()) || StringUtils.isBlank(param.getPk())
                || StringUtils.isBlank(param.getResourceId())) {
            throw new IllegalArgumentException("tableName or resourceId or pk can not be empty");
        }
        return checkResult;
    }


}
