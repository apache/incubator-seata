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

import com.netflix.discovery.converters.Auto;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.vo.BranchSessionVO;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.console.result.PageResult;
import io.seata.server.console.service.GlobalSessionService;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.session.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.seata.common.util.CollectionUtils.isEmpty;
import static io.seata.common.util.CollectionUtils.isNotEmpty;
import static io.seata.common.util.StringUtils.isBlank;
import static io.seata.common.util.StringUtils.isNotBlank;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;

/**
 * Global Session File ServiceImpl
 *
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'file'.equals('${sessionMode}')}")
public class GlobalSessionFileServiceImpl implements GlobalSessionService {

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        if (param.getPageSize() == 0 || param.getPageNum() == 0) {
            return PageResult.success();
        }

        final Collection<GlobalSession> allSessions = SessionHolder.getRootSessionManager().allSessions();


        // calculate pages
        int pages = allSessions.size() / param.getPageSize();
        if (allSessions.size() % param.getPageSize() != 0) {
            pages++;
        }


        final List<GlobalSession> filteredSessions = allSessions
                .stream()
                .filter(obtainPredicate(param))
                .skip((long) Math.max(param.getPageSize(), pages) * (param.getPageNum() - 1))
                .limit(param.getPageSize())
                .collect(Collectors.toList());


        return new PageResult<>(convert(filteredSessions), allSessions.size(), pages, param.getPageNum(), param.getPageSize());
    }

    /**
     * convert GlobalSession to GlobalSessionVO
     * @param filteredSessions the GlobalSession list
     * @return the GlobalSessionVO list
     */
    private List<GlobalSessionVO> convert(List<GlobalSession> filteredSessions) {
        final ArrayList<GlobalSessionVO> result = new ArrayList<>(filteredSessions.size());

        for (GlobalSession session : filteredSessions) {
            result.add(new GlobalSessionVO(
                    session.getXid(),
                    session.getTransactionId(),
                    session.getStatus().getCode(),
                    session.getApplicationId(),
                    session.getTransactionServiceGroup(),
                    session.getTransactionName(),
                    (long) session.getTimeout(),
                    session.getBeginTime(),
                    session.getApplicationData(),
                    convert(session.getBranchSessions())
            ));
        }
        return result;
    }

    /**
     * convert BranchSession to BranchSessionVO
     * @param branchSessions the BranchSession list
     * @return the BranchSessionVO list
     */
    private Set<BranchSessionVO> convert(ArrayList<BranchSession> branchSessions) {
        final Set<BranchSessionVO> result = new HashSet<>(branchSessions.size());

        for (BranchSession session : branchSessions) {
            result.add(new BranchSessionVO(
                    session.getXid(),
                    session.getTransactionId(),
                    session.getBranchId(),
                    session.getResourceGroupId(),
                    session.getResourceId(),
                    session.getBranchType().name(),
                    session.getStatus().getCode(),
                    session.getClientId(),
                    session.getApplicationData()
            ));
        }
        return result;
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
                    (isBlank(param.getXid()) || StringUtils.equals(session.getXid(), param.getXid()))

                    &&
                    // applicationId
                    (isBlank(param.getApplicationId()) ||
                            StringUtils.equals(session.getApplicationId(), param.getApplicationId()))

                    &&
                    // status
                    (isNull(param.getStatus()) || Objects.equals(session.getStatus().getCode(), param.getStatus()))

                    &&
                    // transactionName
                    (isBlank(param.getTransactionName()) ||
                            StringUtils.equals(session.getTransactionName(), param.getTransactionName()))

                    &&
                    // withBranch
                    (param.isWithBranch() ? isNotEmpty(session.getBranchSessions()) : isEmpty(session.getBranchSessions()))

                    &&
                    // timeStart
                    (isNull(param.getTimeStart()) || param.getTimeStart().getTime() <= session.getBeginTime())

                    &&
                    // timeEnd
                    (isNull(param.getTimeEnd()) || param.getTimeEnd().getTime() >= session.getBeginTime());

        };
    }

}
