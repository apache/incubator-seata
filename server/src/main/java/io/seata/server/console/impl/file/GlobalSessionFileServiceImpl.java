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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.seata.server.console.param.GlobalSessionParam;
import io.seata.console.result.PageResult;
import io.seata.server.console.vo.GlobalSessionVO;
import io.seata.server.console.service.GlobalSessionService;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.SessionConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static io.seata.common.util.StringUtils.isBlank;
import static java.util.Objects.isNull;

/**
 * Global Session File ServiceImpl
 *
 * @author zhongxiang.wang
 * @author miaoxueyu
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
                (isNull(param.getTimeStart()) || param.getTimeStart() <= session.getBeginTime())

                &&
                // timeEnd
                (isNull(param.getTimeEnd()) || param.getTimeEnd() >= session.getBeginTime());

        };
    }

}
