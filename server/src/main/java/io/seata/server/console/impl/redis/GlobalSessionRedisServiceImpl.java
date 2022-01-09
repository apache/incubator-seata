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

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.vo.BranchSessionVO;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.console.result.PageResult;
import io.seata.server.console.service.GlobalSessionService;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Global Session Redis ServiceImpl
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'redis'.equals('${sessionMode}')}")
public class GlobalSessionRedisServiceImpl implements GlobalSessionService {

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        List<GlobalSessionVO> result = new ArrayList<>();
        int total = 0;
        RedisTransactionStoreManager instance = RedisTransactionStoreManager.getInstance();
        Integer queryFlag = checkParams(param);
        if (queryFlag == 0){
            total = instance.countByClobalSesisons();
            int pageNum = param.getPageNum() * param.getPageSize() +1;
            List<GlobalSession> globalSessions = instance.findGlobalSessionKeys(pageNum, param.getPageSize());
            convertToVos(result,globalSessions);
        }else{

        }

        return PageResult.success(result,total,param.getPageNum(),param.getPageSize());
    }



    private void convertToVos(List<GlobalSessionVO> result,List<GlobalSession> globalSessions) {
        if (CollectionUtils.isNotEmpty(globalSessions)){
            for (GlobalSession globalSession : globalSessions) {
                GlobalSessionVO globalSessionVO = new GlobalSessionVO();
                BeanUtils.copyProperties(globalSession,globalSessionVO);
                globalSessionVO.setStatus(0);
                globalSessionVO.setTimeout(Long.valueOf(globalSession.getTimeout()));
                globalSessionVO.setBranchSessionVOs(converToBranchSession(globalSession.getBranchSessions()));
                result.add(globalSessionVO);
            }
        }
    }

    private Set<BranchSessionVO> converToBranchSession(ArrayList<BranchSession> branchSessions) {
        Set<BranchSessionVO> branchSessionVOS = new HashSet<>(branchSessions.size());
        if (CollectionUtils.isNotEmpty(branchSessions)){
            for (BranchSession branchSession : branchSessions) {
                BranchSessionVO branchSessionVONew = new BranchSessionVO();
                BeanUtils.copyProperties(branchSession,branchSessionVONew);

                branchSessionVONew.setBranchType(branchSession.getBranchType().name());
                branchSessionVONew.setStatus(branchSession.getStatus().getCode());

                branchSessionVONew.setGmtCreate(null);
                branchSessionVONew.setGmtModified(null);

                branchSessionVOS.add(branchSessionVONew);
            }
        }
        return branchSessionVOS;
    }


    /**
     *
     * @param param
     * @return
     */
    private Integer checkParams(GlobalSessionParam param) {
        Integer queryFlag = 1;
        if (param.getPageNum() == 0 || param.getPageSize() == 0){
            param.setPageNum(0);
            param.setPageSize(20);
        }

        if (StringUtils.isBlank(param.getXid()) && StringUtils.isBlank(param.getApplicationId())
                && param.getStatus() == null){
            queryFlag =  0;
        }

        return queryFlag;
    }

}
