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
package org.apache.seata.server.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import org.apache.seata.server.console.vo.BranchSessionVO;
import org.apache.seata.server.console.vo.GlobalSessionVO;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.store.SessionStorable;
import org.springframework.beans.BeanUtils;

/**
 * The session converter
 *
 */
public class SessionConverter {

    public static GlobalSession convertGlobalSession(GlobalTransactionDO globalTransactionDO, boolean lazyLoadBranch) {
        if (globalTransactionDO == null) {
            return null;
        }
        GlobalSession session = new GlobalSession(globalTransactionDO.getApplicationId(),
                globalTransactionDO.getTransactionServiceGroup(),
                globalTransactionDO.getTransactionName(),
                globalTransactionDO.getTimeout(), lazyLoadBranch);
        session.setXid(globalTransactionDO.getXid());
        session.setTransactionId(globalTransactionDO.getTransactionId());
        session.setStatus(GlobalStatus.get(globalTransactionDO.getStatus()));
        session.setApplicationData(globalTransactionDO.getApplicationData());
        session.setBeginTime(globalTransactionDO.getBeginTime());
        return session;
    }

    public static GlobalSession convertGlobalSession(GlobalTransactionDO globalTransactionDO) {
        return convertGlobalSession(globalTransactionDO, false);
    }

    public static BranchSession convertBranchSession(BranchTransactionDO branchTransactionDO) {
        if (branchTransactionDO == null) {
            return null;
        }
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(branchTransactionDO.getXid());
        branchSession.setTransactionId(branchTransactionDO.getTransactionId());
        branchSession.setApplicationData(branchTransactionDO.getApplicationData());
        branchSession.setBranchId(branchTransactionDO.getBranchId());
        branchSession.setBranchType(BranchType.valueOf(branchTransactionDO.getBranchType()));
        branchSession.setResourceId(branchTransactionDO.getResourceId());
        branchSession.setClientId(branchTransactionDO.getClientId());
        branchSession.setResourceGroupId(branchTransactionDO.getResourceGroupId());
        branchSession.setStatus(BranchStatus.get(branchTransactionDO.getStatus()));
        if (branchTransactionDO instanceof BranchTransactionDTO) {
            branchSession.setLockKey(((BranchTransactionDTO)branchTransactionDO).getLockKey());
        }
        return branchSession;
    }

    public static GlobalTransactionDO convertGlobalTransactionDO(SessionStorable session) {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        convertGlobalTransactionDO(globalTransactionDO, session);
        return globalTransactionDO;
    }

    public static void convertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO,
        SessionStorable session) {
        if (!(session instanceof GlobalSession)) {
            throw new IllegalArgumentException(
                "The parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        GlobalSession globalSession = (GlobalSession)session;
        globalTransactionDO.setXid(globalSession.getXid());
        globalTransactionDO.setStatus(globalSession.getStatus().getCode());
        globalTransactionDO.setApplicationId(globalSession.getApplicationId());
        globalTransactionDO.setBeginTime(globalSession.getBeginTime());
        globalTransactionDO.setTimeout(globalSession.getTimeout());
        globalTransactionDO.setTransactionId(globalSession.getTransactionId());
        globalTransactionDO.setTransactionName(globalSession.getTransactionName());
        globalTransactionDO.setTransactionServiceGroup(globalSession.getTransactionServiceGroup());
        globalTransactionDO.setApplicationData(globalSession.getApplicationData());
    }

    public static BranchTransactionDO convertBranchTransactionDO(SessionStorable session) {
        if (!(session instanceof BranchSession)) {
            throw new IllegalArgumentException(
                    "The parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        convertBranchTransaction(branchTransactionDO, session);
        return branchTransactionDO;
    }

    public static BranchTransactionDTO convertBranchTransactionDTO(SessionStorable session) {
        if (!(session instanceof BranchSession)) {
            throw new IllegalArgumentException(
                "The parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        BranchTransactionDTO branchTransactionDTO = new BranchTransactionDTO();
        convertBranchTransaction(branchTransactionDTO, session);
        return branchTransactionDTO;
    }

    public static void convertBranchTransaction(BranchTransactionDO branchTransactionDO, SessionStorable session) {
        BranchSession branchSession = (BranchSession)session;
        branchTransactionDO.setXid(branchSession.getXid());
        branchTransactionDO.setBranchId(branchSession.getBranchId());
        branchTransactionDO.setBranchType(branchSession.getBranchType().name());
        branchTransactionDO.setClientId(branchSession.getClientId());
        branchTransactionDO.setResourceGroupId(branchSession.getResourceGroupId());
        branchTransactionDO.setTransactionId(branchSession.getTransactionId());
        branchTransactionDO.setApplicationData(branchSession.getApplicationData());
        branchTransactionDO.setResourceId(branchSession.getResourceId());
        branchTransactionDO.setStatus(branchSession.getStatus().getCode());
        if (branchTransactionDO instanceof BranchTransactionDTO) {
            ((BranchTransactionDTO)branchTransactionDO).setLockKey(branchSession.getLockKey());
        }
    }

    public static void convertToGlobalSessionVo(List<GlobalSessionVO> result, List<GlobalSession> globalSessions) {
        if (CollectionUtils.isNotEmpty(globalSessions)) {
            for (GlobalSession globalSession : globalSessions) {
                GlobalSessionVO globalSessionVO = new GlobalSessionVO();
                BeanUtils.copyProperties(globalSession,globalSessionVO);
                globalSessionVO.setStatus(globalSession.getStatus().getCode());
                globalSessionVO.setTimeout(Long.valueOf(globalSession.getTimeout()));
                globalSessionVO.setBranchSessionVOs(converToBranchSession(globalSession.getBranchSessions()));
                result.add(globalSessionVO);
            }
        }
    }

    public static Set<BranchSessionVO> converToBranchSession(List<BranchSession> branchSessions) {
        Set<BranchSessionVO> branchSessionVOs = new HashSet<>(branchSessions.size());
        if (CollectionUtils.isNotEmpty(branchSessions)) {
            for (BranchSession branchSession : branchSessions) {
                BranchSessionVO branchSessionVONew = new BranchSessionVO();
                BeanUtils.copyProperties(branchSession,branchSessionVONew);

                branchSessionVONew.setBranchType(branchSession.getBranchType().name());
                branchSessionVONew.setStatus(branchSession.getStatus().getCode());
                branchSessionVOs.add(branchSessionVONew);
            }
        }
        return branchSessionVOs;
    }

    /**
     * convert GlobalSession to GlobalSessionVO
     *
     * @param filteredSessions the GlobalSession list
     * @return the GlobalSessionVO list
     */
    public static List<GlobalSessionVO> convertGlobalSession(List<GlobalSession> filteredSessions) {

        if (CollectionUtils.isEmpty(filteredSessions)) {
            return Collections.emptyList();
        }

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
                    convertBranchSession(session.getBranchSessions())
            ));
        }
        return result;
    }

    /**
     * convert BranchSession to BranchSessionVO
     *
     * @param branchSessions the BranchSession list
     * @return the BranchSessionVO list
     */
    public static Set<BranchSessionVO> convertBranchSession(List<BranchSession> branchSessions) {

        if (CollectionUtils.isEmpty(branchSessions)) {
            return Collections.emptySet();
        }

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

}
