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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.seata.common.exception.InvalidParamException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.console.param.GlobalLockParam;
import io.seata.core.console.vo.GlobalLockVO;
import io.seata.core.console.result.PageResult;
import io.seata.core.lock.RowLock;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static io.seata.core.console.vo.GlobalLockVO.convert;
import static io.seata.common.util.StringUtils.isBlank;

/**
 * Global Lock File ServiceImpl
 *
 * @author zhongxiang.wang
 * @author miaoxueyu
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'file'.equals('${lockMode}')}")
public class GlobalLockFileServiceImpl implements GlobalLockService {

    /**
     * The constant LOCK_SPLIT.
     */
    protected static final String LOCK_SPLIT = "^^^";

    @Override
    public PageResult<GlobalLockVO> query(GlobalLockParam param) {
        checkParam(param);

        final Collection<GlobalSession> allSessions = SessionHolder.getRootSessionManager().allSessions();
        if (CollectionUtils.isEmpty(allSessions)) {
            return PageResult.success();
        }

        final AtomicInteger total = new AtomicInteger();
        List<RowLock> result = allSessions
                .parallelStream()
                .filter(obtainGlobalSessionPredicate(param))
                .flatMap(globalSession -> globalSession.getBranchSessions().stream())
                .filter(obtainBranchSessionPredicate(param))
                .flatMap(branchSession -> filterAndMap(param, branchSession))
                .peek(globalSession -> total.incrementAndGet())
                .skip((long) param.getPageSize() * (param.getPageNum() - 1))
                .limit(param.getPageSize())
                .collect(Collectors.toList());

        // calculate pages
        int pages = total.get() / param.getPageSize();
        if (total.get() % param.getPageSize() != 0) {
            pages++;
        }

        return new PageResult<>(convert(result), total.get(), pages, param.getPageNum(), param.getPageSize());

    }

    /**
     * filter with tableName and generate RowLock
     *
     * @param param
     * @param branchSession the branch session
     * @return the RowLock list
     */
    private Stream<RowLock> filterAndMap(GlobalLockParam param, BranchSession branchSession) {

        List<RowLock> locks = new ArrayList<>();
        String[] tableGroupedLockKeys = branchSession.getLockKey().split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                return Stream.empty();
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            // filter by table name
            if (StringUtils.isNotBlank(param.getTableName()) && !tableName.contains(param.getTableName())) {
                continue;
            }
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            if (StringUtils.isBlank(mergedPKs)) {
                return Stream.empty();
            }
            String[] pks = mergedPKs.split(",");
            if (pks.length == 0) {
                return Stream.empty();
            }
            for (String pk : pks) {
                if (StringUtils.isNotBlank(pk)) {
                    RowLock rowLock = new RowLock();
                    rowLock.setXid(branchSession.getXid());
                    rowLock.setTransactionId(branchSession.getTransactionId());
                    rowLock.setBranchId(branchSession.getBranchId());
                    rowLock.setTableName(tableName);
                    rowLock.setPk(pk);
                    rowLock.setResourceId(branchSession.getResourceId());
                    rowLock.setRowKey(getRowKey(branchSession.getResourceId(), tableName, pk));
                    locks.add(rowLock);
                }
            }
        }
        return locks.stream();
    }

    /**
     * Get row key string.
     *
     * @param resourceId the resource id
     * @param tableName  the table name
     * @param pk         the pk
     * @return the string
     */
    protected String getRowKey(String resourceId, String tableName, String pk) {
        return resourceId + LOCK_SPLIT + tableName + LOCK_SPLIT + pk;
    }


    /**
     * Collect row locks list.
     *
     * @param lockKey       the lock key
     * @param resourceId    the resource id
     * @param xid           the xid
     * @param transactionId the transaction id
     * @param branchID      the branch id
     * @return the list
     */
    protected List<RowLock> collectRowLocks(String lockKey, String resourceId, String xid, Long transactionId,
                                            Long branchID) {
        List<RowLock> locks = new ArrayList<>();

        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                return locks;
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            if (StringUtils.isBlank(mergedPKs)) {
                return locks;
            }
            String[] pks = mergedPKs.split(",");
            if (pks.length == 0) {
                return locks;
            }
            for (String pk : pks) {
                if (StringUtils.isNotBlank(pk)) {
                    RowLock rowLock = new RowLock();
                    rowLock.setXid(xid);
                    rowLock.setTransactionId(transactionId);
                    rowLock.setBranchId(branchID);
                    rowLock.setTableName(tableName);
                    rowLock.setPk(pk);
                    rowLock.setResourceId(resourceId);
                    locks.add(rowLock);
                }
            }
        }
        return locks;
    }

    /**
     * check the param
     *
     * @param param the param
     */
    private void checkParam(GlobalLockParam param) {
        if (param.getPageSize() <= 0 || param.getPageNum() <= 0) {
            throw new InvalidParamException("wrong pageSize or pageNum");
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
                    (isNull(param.getTimeStart()) || param.getTimeStart().getTime() <= globalSession.getBeginTime())

                    &&
                    // timeEnd
                    (isNull(param.getTimeEnd()) || param.getTimeEnd().getTime() >= globalSession.getBeginTime());
        };
    }


}
