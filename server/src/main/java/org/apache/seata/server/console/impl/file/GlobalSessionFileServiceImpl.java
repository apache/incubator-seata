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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.seata.server.console.param.GlobalSessionParam;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.server.console.vo.GlobalSessionVO;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.storage.SessionConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.util.StringUtils.isBlank;
import static java.util.Objects.isNull;

/**
 * Global Session File ServiceImpl
 *
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'file'.equals('${sessionMode}')}")
public class GlobalSessionFileServiceImpl implements GlobalSessionService {

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        if (param.getPageSize() <= 0 || param.getPageNum() <= 0) {
            throw new IllegalArgumentException("wrong pageSize or pageNum");
        }

        final Collection<GlobalSession> allSessions = SessionHolder.getRootSessionManager().allSessions();

        final List<GlobalSession> filteredSessions = allSessions
                .parallelStream()
                .filter(obtainPredicate(param))
                .collect(Collectors.toList());

        return PageResult.build(SessionConverter.convertGlobalSession(filteredSessions), param.getPageNum(), param.getPageSize());
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
                // applicationId
                (isBlank(param.getApplicationId()) || session.getApplicationId().contains(param.getApplicationId()))

                &&
                // status
                (isNull(param.getStatus()) || Objects.equals(session.getStatus().getCode(), param.getStatus()))

                &&
                // transactionName
                (isBlank(param.getTransactionName()) || session.getTransactionName().contains(param.getTransactionName()))

                &&
                // timeStart
                (isNull(param.getTimeStart()) || param.getTimeStart() / 1000 >= session.getBeginTime() / 1000)

                &&
                // timeEnd
                (isNull(param.getTimeEnd()) || param.getTimeEnd() / 1000 <= session.getBeginTime() / 1000);

        };
    }


}
