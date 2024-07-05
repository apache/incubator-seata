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
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.server.console.vo.BranchSessionVO;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.server.console.service.BranchSessionService;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManagerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Branch Session Redis ServiceImpl
 *
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'redis'.equals('${sessionMode}')}")
public class BranchSessionRedisServiceImpl implements BranchSessionService {

    @Override
    public PageResult<BranchSessionVO> queryByXid(String xid) {
        if (StringUtils.isBlank(xid)) {
            return PageResult.success();
        }

        List<BranchSessionVO> branchSessionVos = new ArrayList<>();

        RedisTransactionStoreManager instance = RedisTransactionStoreManagerFactory.getInstance();

        List<BranchTransactionDO> branchSessionDos = instance.findBranchSessionByXid(xid);

        if (CollectionUtils.isNotEmpty(branchSessionDos)) {
            for (BranchTransactionDO branchSessionDo : branchSessionDos) {
                BranchSessionVO branchSessionVO = new BranchSessionVO();
                BeanUtils.copyProperties(branchSessionDo, branchSessionVO);
                branchSessionVos.add(branchSessionVO);
            }
        }

        return PageResult.success(branchSessionVos, branchSessionVos.size(), 0, branchSessionVos.size());
    }
}
