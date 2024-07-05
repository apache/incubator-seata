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
package org.apache.seata.server.console.impl.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.server.console.param.GlobalSessionParam;
import org.apache.seata.server.console.vo.GlobalSessionVO;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import static org.apache.seata.common.exception.FrameworkErrorCode.ParameterRequired;
import static org.apache.seata.common.util.StringUtils.isBlank;
import static org.apache.seata.common.util.StringUtils.isNotBlank;
import static org.apache.seata.common.result.PageResult.checkPage;
import static org.apache.seata.server.storage.SessionConverter.convertToGlobalSessionVo;

/**
 * Global Session Redis ServiceImpl
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'redis'.equals('${sessionMode}')}")
public class GlobalSessionRedisServiceImpl implements GlobalSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionRedisServiceImpl.class);

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        List<GlobalSessionVO> result = new ArrayList<>();
        Long total = 0L;
        if (param.getTimeStart() != null || param.getTimeEnd() != null) {
            //not support time range query
            LOGGER.debug("not supported according to time range query");
            return PageResult.failure(ParameterRequired.getErrCode(),"not supported according to time range query");
        }
        List<GlobalSession> globalSessions = new ArrayList<>();

        RedisTransactionStoreManager instance = RedisTransactionStoreManagerFactory.getInstance();

        checkPage(param);

        if (isBlank(param.getXid()) && param.getStatus() == null) {
            total = instance.countByGlobalSessions(GlobalStatus.values());
            globalSessions = instance.findGlobalSessionByPage(param.getPageNum(), param.getPageSize(),param.isWithBranch());
        } else {
            List<GlobalSession> globalSessionsNew = new ArrayList<>();
            if (isNotBlank(param.getXid())) {
                SessionCondition sessionCondition = new SessionCondition();
                sessionCondition.setXid(param.getXid());
                sessionCondition.setLazyLoadBranch(!param.isWithBranch());
                globalSessions = instance.readSession(sessionCondition);
                total = (long)globalSessions.size();
            }

            if (param.getStatus() != null && GlobalStatus.get(param.getStatus()) != null) {
                if (CollectionUtils.isNotEmpty(globalSessions)) {
                    globalSessionsNew = globalSessions.stream().filter(globalSession -> globalSession.getStatus().getCode() == (param.getStatus())).collect(Collectors.toList());
                    total = (long)globalSessionsNew.size();
                } else {
                    total = instance.countByGlobalSessions(new GlobalStatus[] {GlobalStatus.get(param.getStatus())});
                    globalSessionsNew = instance.readSessionStatusByPage(param);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                if (isNotBlank(param.getApplicationId())) {
                    //not support
                    LOGGER.debug("not supported according to applicationId query");
                }
                if (isNotBlank(param.getTransactionName())) {
                    //not support
                    LOGGER.debug("not supported according to transactionName query");
                }
            }
            globalSessions = globalSessionsNew.size() > 0 ? globalSessionsNew : globalSessions;
        }

        convertToGlobalSessionVo(result,globalSessions);

        return PageResult.success(result,total.intValue(),param.getPageNum(),param.getPageSize());
    }

}
