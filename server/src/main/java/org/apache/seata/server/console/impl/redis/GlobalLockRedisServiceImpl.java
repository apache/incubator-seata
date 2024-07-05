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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.seata.common.util.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.apache.seata.common.util.BeanUtils;
import org.apache.seata.server.console.param.GlobalLockParam;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.server.console.vo.GlobalLockVO;
import org.apache.seata.server.console.service.GlobalLockService;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;
import static org.apache.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;
import static org.apache.seata.common.exception.FrameworkErrorCode.ParameterRequired;
import static org.apache.seata.common.util.StringUtils.isNotBlank;
import static org.apache.seata.common.result.PageResult.checkPage;
import static org.apache.seata.core.constants.RedisKeyConstants.DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX;
import static org.apache.seata.core.constants.RedisKeyConstants.DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX;
import static org.apache.seata.core.constants.RedisKeyConstants.SPLIT;

/**
 * Global Lock Redis Service Impl
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'redis'.equals('${lockMode}')}")
public class GlobalLockRedisServiceImpl implements GlobalLockService {

    @Override
    public PageResult<GlobalLockVO> query(GlobalLockParam param) {

        int total = 0;
        List<GlobalLockVO> globalLockVos;
        checkPage(param);
        if (isNotBlank(param.getXid())) {
            globalLockVos = queryGlobalByXid(param.getXid());
            total = globalLockVos.size();
            return PageResult.success(globalLockVos,total,param.getPageNum(),param.getPageSize());
        } else if (isNotBlank(param.getTableName()) && isNotBlank(param.getPk()) && isNotBlank(param.getResourceId())) {
            //SEATA_ROW_LOCK_jdbc:mysql://116.62.62.26/seata-order^^^order^^^2188
            String tableName = param.getTableName();
            String pk = param.getPk();
            String resourceId = param.getResourceId();
            globalLockVos = queryGlobalLockByRowKey(buildRowKey(tableName,pk,resourceId));
            total = globalLockVos.size();
            return PageResult.success(globalLockVos,total,param.getPageNum(),param.getPageSize());
        } else {
            return PageResult.failure(ParameterRequired.getErrCode(),"only three parameters of tableName,pk,resourceId or Xid are supported");
        }
    }

    private List<GlobalLockVO> queryGlobalLockByRowKey(String buildRowKey) {
        return readGlobalLockByRowKey(buildRowKey);
    }

    private String buildRowKey(String tableName, String pk,String resourceId) {
        return DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX + resourceId + SPLIT + tableName + SPLIT + pk;
    }


    private List<GlobalLockVO> queryGlobalByXid(String xid) {
        return readGlobalLockByXid(DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX + xid);
    }

    private List<GlobalLockVO> readGlobalLockByXid(String key) {
        List<GlobalLockVO> vos = new ArrayList<>();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Map<String, String> mapGlobalKeys = jedis.hgetAll(key);
            if (CollectionUtils.isNotEmpty(mapGlobalKeys)) {
                List<String> rowLockKeys = new ArrayList<>();
                mapGlobalKeys.forEach((k,v) -> rowLockKeys.addAll(Arrays.asList(v.split(ROW_LOCK_KEY_SPLIT_CHAR))));
                for (String rowLoclKey : rowLockKeys) {
                    Map<String, String> mapRowLockKey = jedis.hgetAll(rowLoclKey);
                    GlobalLockVO vo = (GlobalLockVO)BeanUtils.mapToObject(mapRowLockKey, GlobalLockVO.class);
                    if (vo != null) {
                        vos.add(vo);
                    }
                }
            }
        }

        return vos;
    }


    private List<GlobalLockVO> readGlobalLockByRowKey(String key) {
        List<GlobalLockVO> vos = new ArrayList<>();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Map<String, String> map = jedis.hgetAll(key);
            GlobalLockVO vo = (GlobalLockVO)BeanUtils.mapToObject(map, GlobalLockVO.class);
            if (vo != null) {
                vos.add(vo);
            }
        }
        return vos;
    }

}
