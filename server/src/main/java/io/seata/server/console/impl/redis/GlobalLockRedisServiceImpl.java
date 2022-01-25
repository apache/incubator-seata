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


import java.util.List;
import java.util.Map;
import com.google.common.collect.Lists;
import io.seata.common.util.BeanUtils;
import io.seata.core.console.param.GlobalLockParam;
import io.seata.core.console.vo.GlobalLockVO;
import io.seata.core.console.result.PageResult;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.storage.redis.JedisPooledFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import javax.annotation.Resource;
import static io.seata.common.exception.FrameworkErrorCode.ParameterRequired;
import static io.seata.common.util.StringUtils.isNotBlank;
import static io.seata.core.console.result.PageResult.checkPage;
import static io.seata.core.constants.RedisKeyConstants.*;

/**
 * Global Lock Redis Service Impl
 * @author: zhongxiang.wang
 * @author: doubleDimple
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'redis'.equals('${lockMode}')}")
public class GlobalLockRedisServiceImpl implements GlobalLockService {

    private Logger logger = LoggerFactory.getLogger(GlobalLockRedisServiceImpl.class);

    @Resource
    private io.seata.core.model.Resource resource;

    @Override
    public PageResult<GlobalLockVO> query(GlobalLockParam param) {

        int total = 0;
        List<GlobalLockVO> globalLockVos;

        checkPage(param);

        if (isNotBlank(param.getXid())) {
            globalLockVos = queryGlobalByXid(param.getXid());
            total = globalLockVos.size();
            return PageResult.success(globalLockVos,total,param.getPageNum(),param.getPageSize());
        } else if (isNotBlank(param.getTableName()) && isNotBlank(param.getPk())) {
            //SEATA_ROW_LOCK_jdbc:mysql://116.62.62.26/seata-order^^^order^^^2188
            String tableName = param.getTableName();
            String pk = param.getPk();
            globalLockVos = queryGlobalLockByRowKey(buildRowKey(tableName,pk));
            return PageResult.success(globalLockVos,total,param.getPageNum(),param.getPageSize());
        } else {
            return PageResult.failure(ParameterRequired.getErrCode(),"not support request param");
        }
    }

    private List<GlobalLockVO> queryGlobalLockByRowKey(String buildRowKey) {
        return readGlobalLock(buildRowKey);
    }

    private String buildRowKey(String tableName, String pk) {
        String resourceId = resource.getResourceId();
        return DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX + resourceId + SPLIT + tableName + SPLIT + pk;
    }


    private List<GlobalLockVO> queryGlobalByXid(String xid) {
       return readGlobalLock(DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX + xid);
    }


    private List<GlobalLockVO> readGlobalLock(String key) {
        List<GlobalLockVO> vos = Lists.newArrayList();
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
