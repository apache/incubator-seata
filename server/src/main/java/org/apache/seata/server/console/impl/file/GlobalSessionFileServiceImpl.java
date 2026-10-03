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

import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.console.entity.param.GlobalSessionParam;
import org.apache.seata.server.console.entity.vo.GlobalSessionVO;
import org.apache.seata.server.console.impl.AbstractGlobalService;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.SessionConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.apache.seata.common.util.StringUtils.isBlank;
import static org.apache.seata.common.util.StringUtils.isNotBlank;

/**
 * Global Session File ServiceImpl
 *
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'file'.equals('${sessionMode}')}")
public class GlobalSessionFileServiceImpl extends AbstractGlobalService implements GlobalSessionService {

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        if (param.getPageSize() <= 0 || param.getPageNum() <= 0) {
            throw new IllegalArgumentException("wrong pageSize or pageNum");
        }

        final Collection<GlobalSession> allSessions = findCandidateSessions(param);

        final List<GlobalSession> filteredSessions =
                allSessions.parallelStream().filter(obtainPredicate(param)).collect(Collectors.toList());

        return PageResult.build(
                SessionConverter.convertGlobalSession(filteredSessions), param.getPageNum(), param.getPageSize());
    }

    private Collection<GlobalSession> findCandidateSessions(GlobalSessionParam param) {
        SessionManager sessionManager = SessionHolder.getRootSessionManager();
        if (isNotBlank(param.getXid()) && isCompleteXid(param.getXid())) {
            GlobalSession globalSession = sessionManager.findGlobalSession(param.getXid(), param.isWithBranch());
            return globalSession == null ? Collections.emptyList() : Collections.singletonList(globalSession);
        }
        if (param.getTransactionId() != null && param.getTransactionId() > 0) {
            SessionCondition sessionCondition = new SessionCondition();
            sessionCondition.setTransactionId(param.getTransactionId());
            sessionCondition.setLazyLoadBranch(!param.isWithBranch());
            return sessionManager.findGlobalSessions(sessionCondition);
        }
        if (param.getStatus() != null) {
            GlobalStatus globalStatus = getGlobalStatus(param.getStatus());
            if (globalStatus == null) {
                return Collections.emptyList();
            }
            SessionCondition sessionCondition = new SessionCondition(globalStatus);
            sessionCondition.setLazyLoadBranch(!param.isWithBranch());
            return sessionManager.findGlobalSessions(sessionCondition);
        }
        return sessionManager.allSessions();
    }

    private GlobalStatus getGlobalStatus(int status) {
        try {
            return GlobalStatus.get(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isCompleteXid(String xid) {
        int lastSplitIndex = xid.lastIndexOf(':');
        if (lastSplitIndex <= 0 || lastSplitIndex == xid.length() - 1) {
            return false;
        }
        int portSplitIndex = xid.lastIndexOf(':', lastSplitIndex - 1);
        if (portSplitIndex <= 0 || portSplitIndex == lastSplitIndex - 1) {
            return false;
        }
        try {
            Integer.parseInt(xid.substring(portSplitIndex + 1, lastSplitIndex));
            Long.parseLong(xid.substring(lastSplitIndex + 1));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * obtain the condition
     *
     * @param param condition for query global session
     * @return the filter condition
     */
    private Predicate<? super GlobalSession> obtainPredicate(GlobalSessionParam param) {

        return session -> {
            return
            // xid
            (isBlank(param.getXid()) || session.getXid().contains(param.getXid()))
                    &&
                    // transactionId
                    (param.getTransactionId() == null
                            || param.getTransactionId() <= 0
                            || Objects.equals(session.getTransactionId(), param.getTransactionId()))
                    &&
                    // applicationId
                    (isBlank(param.getApplicationId())
                            || session.getApplicationId().contains(param.getApplicationId()))
                    &&
                    // status
                    (isNull(param.getStatus())
                            || Objects.equals(session.getStatus().getCode(), param.getStatus()))
                    &&
                    // transactionName
                    (isBlank(param.getTransactionName())
                            || session.getTransactionName().contains(param.getTransactionName()))
                    &&

                    // vgroup
                    (isBlank(param.getVgroup())
                            || session.getTransactionServiceGroup().equals(param.getVgroup()))
                    &&
                    // timeStart
                    (isNull(param.getTimeStart()) || param.getTimeStart() / 1000 >= session.getBeginTime() / 1000)
                    &&
                    // timeEnd
                    (isNull(param.getTimeEnd()) || param.getTimeEnd() / 1000 <= session.getBeginTime() / 1000);
        };
    }
}
