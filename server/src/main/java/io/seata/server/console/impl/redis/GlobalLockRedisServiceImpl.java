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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import io.seata.common.XID;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.console.param.GlobalLockParam;
import io.seata.core.console.vo.GlobalLockVO;
import io.seata.core.console.result.PageResult;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.storage.redis.JedisPooledFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;


import static io.seata.common.util.StringUtils.isBlank;
import static io.seata.common.util.StringUtils.isNotBlank;

/**
 * Global Lock Redis Service Impl
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'redis'.equals('${lockMode}')}")
public class GlobalLockRedisServiceImpl implements GlobalLockService {

    private static final String DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX = "SEATA_GLOBAL_LOCK";
    private static final String DEFAULT_REDIS_SEATA_GLOBAL_LOCK_KEYS = DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX+"*";

    @Override
    public PageResult<GlobalLockVO> query(GlobalLockParam param) {
        int queryFlag = checkParams(param);
        int total;
        List<GlobalLockVO> globalLockVOs;

        if (queryFlag == 0){
            //query all
            globalLockVOs = queryAllPage(param.getPageNum(),param.getPageSize());
            total = queryAllTotal();
        }else {
            globalLockVOs = queryGlobalByParam(param);
             total = globalLockVOs.size();
        }
        return PageResult.success(globalLockVOs,total,param.getPageNum(),param.getPageSize());
    }

    private int queryAllTotal() {
        int total = 0;
        String cursor = String.valueOf(total);
        ScanParams sp = new ScanParams();
        sp.match(DEFAULT_REDIS_SEATA_GLOBAL_LOCK_KEYS);
        while (true) {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                ScanResult<String> scan = jedis.scan(ScanParams.SCAN_POINTER_START, sp);
                List<String> list = scan.getResult();
                for(int i = 0;i < list.size();i++){
                    total++;
                }
            }
            if (ScanParams.SCAN_POINTER_START.equals(cursor)){
                return total;
            }
        }
    }

    private List<GlobalLockVO> queryAllPage(int pageNum,int pageSize) {
        int start = (pageNum - 1) * pageSize <0 ? 0 : (pageNum - 1) * pageSize;
        int end = pageNum * pageSize;

        Set<String> keys = new HashSet<>();
        String cursor = String.valueOf(start);
        ScanParams sp = new ScanParams();
        sp.match(DEFAULT_REDIS_SEATA_GLOBAL_LOCK_KEYS);
        sp.count(end);

        while (true) {

            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {

                ScanResult<String> scan = jedis.scan(cursor, sp);
                cursor = scan.getCursor();
                List<String> list = scan.getResult();
                for(int i = 0;i < list.size();i++){
                    keys.add(list.get(i));
                    if (keys.size() == pageSize){
                        return readGlobalocks(keys);
                    }
                }
            }

            if (ScanParams.SCAN_POINTER_START.equals(cursor)){
                return readGlobalocks(keys);
            }
        }
    }

    private List<GlobalLockVO> readGlobalocks(Set<String> keys) {
        List<GlobalLockVO> vos = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(keys)){
            try(Jedis jedis = JedisPooledFactory.getJedisInstance()){
                for (String key : keys) {
                    Map<String, String> map = jedis.hgetAll(key);
                    GlobalLockVO vo = (GlobalLockVO)BeanUtils.mapToObject(map, GlobalLockVO.class);
                    if (vo != null){
                        vos.add(vo);
                    }
                }
            }
        }
        return vos;
    }

    private List<GlobalLockVO> queryGlobalByParam(GlobalLockParam param) {
        String xidStr = param.getXid();
        String transactionId = param.getTransactionId();
        List<GlobalLockVO> vos = Lists.newArrayList();

        xidStr = isNotBlank(xidStr) ? xidStr : StringUtils.EMPTY;
        xidStr = isBlank(xidStr) && isNotBlank(transactionId) ? XID.generateXID(Long.valueOf(param.getTransactionId())) : StringUtils.EMPTY;
        xidStr = isNotBlank(xidStr) && isNotBlank(transactionId) ? getxidStr(param) : StringUtils.EMPTY;


        String key = DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX+xidStr;
        if (isNotBlank(xidStr)){
            try(Jedis jedis = JedisPooledFactory.getJedisInstance()){
                Map<String, String> map = jedis.hgetAll(key);
                GlobalLockVO vo = (GlobalLockVO)BeanUtils.mapToObject(map, GlobalLockVO.class);
                if (vo != null){
                    vos.add(vo);
                }
            }
        }

        return vos;
    }

    private String getxidStr(GlobalLockParam param) {
        String xid = XID.generateXID(Long.valueOf(param.getTransactionId()));
        if (!xid.equals(param.getXid())){
            return StringUtils.EMPTY;
        }else {
            return xid;
        }
    }


    private int checkParams(GlobalLockParam param) {
        if (param.getPageSize() <= 0){
            param.setPageSize(20);
        }
        if (param.getPageNum() <= 0){
            param.setPageNum(1);
        }

        if (isNotBlank(param.getXid()) || isNotBlank(param.getTransactionId())){
            return 1;
        }else{
            //not support other query param
            return 0;
        }
    }

}
