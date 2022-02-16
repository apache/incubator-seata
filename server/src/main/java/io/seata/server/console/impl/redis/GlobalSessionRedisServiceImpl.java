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
package io.seata.server.console.impl.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import io.seata.common.util.CollectionUtils;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.console.result.PageResult;
import io.seata.core.model.GlobalStatus;
import io.seata.server.console.service.GlobalSessionService;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import static io.seata.common.util.StringUtils.isBlank;
import static io.seata.common.util.StringUtils.isNotBlank;
import static io.seata.core.console.result.PageResult.checkPage;
import static io.seata.server.storage.SessionConverter.convertToGlobalSessionVo;

/**
 * Global Session Redis ServiceImpl
 * @author: zhongxiang.wang
 * @author: doubleDimple
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'redis'.equals('${sessionMode}')}")
public class GlobalSessionRedisServiceImpl implements GlobalSessionService {

    private Logger logger = LoggerFactory.getLogger(GlobalSessionRedisServiceImpl.class);

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        List<GlobalSessionVO> result = new ArrayList<>();
        Long total = 0L;
        if (param.getTimeStart() != null || param.getTimeEnd() != null) {
            //not support time range query
            logger.debug("not supported according to time range query");
            return PageResult.success(result,0,param.getPageNum(),param.getPageSize());
        }
        List<GlobalSession> globalSessions = new ArrayList<>();

        RedisTransactionStoreManager instance = RedisTransactionStoreManager.getInstance();

        checkPage(param);

        if (isBlank(param.getXid()) && param.getStatus() == null) {
            total = instance.countByClobalSesisons(GlobalStatus.values());
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
                    total = instance.countByClobalSesisons(new GlobalStatus[]{GlobalStatus.get(param.getStatus())});
                    globalSessionsNew = instance.readSessionStatusByPage(param);
                }
            }

            if (isNotBlank(param.getApplicationId())) {
                //not support
                logger.debug("not supported according to applicationId query");
            }
            if (isNotBlank(param.getTransactionName())) {
                //not support
                logger.debug("not supported according to transactionName query");
            }
            globalSessions = globalSessionsNew.size() > 0 ? globalSessionsNew : globalSessions;
        }
        convertToGlobalSessionVo(result,globalSessions);

        return PageResult.success(result,total.intValue(),param.getPageNum(),param.getPageSize());
    }

}
