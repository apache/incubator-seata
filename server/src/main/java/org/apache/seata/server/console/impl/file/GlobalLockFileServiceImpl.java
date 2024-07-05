/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.console.impl.file;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.console.param.GlobalLockParam;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.server.console.vo.GlobalLockVO;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.server.console.service.GlobalLockService;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.util.StringUtils.isBlank;
import static org.apache.seata.server.console.vo.GlobalLockVO.convert;
import static java.util.Objects.isNull;

/**
 * Global Lock File ServiceImpl
 *
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'file'.equals('${lockMode}')}")
public class GlobalLockFileServiceImpl implements GlobalLockService {

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

    /**
     * filter with tableName and generate RowLock
     *
     * @param param the query param
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
                    (isNull(param.getTimeStart()) || param.getTimeStart() / 1000 >= globalSession.getBeginTime() / 1000)

                    &&
                    // timeEnd
                    (isNull(param.getTimeEnd()) || param.getTimeEnd() / 1000 <= globalSession.getBeginTime() / 1000);

        };
    }

}
