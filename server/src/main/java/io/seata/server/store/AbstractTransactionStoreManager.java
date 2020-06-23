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
package io.seata.server.store;

import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.LogStore;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type Abstract transaction store manager.
 *
 * @author zhangsen
 */
public abstract class AbstractTransactionStoreManager implements TransactionStoreManager {

    //region Fields

    /**
     * The constant DEFAULT_LOG_QUERY_LIMIT.
     */
    public static final int DEFAULT_LOG_QUERY_LIMIT = 100;

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The Log store.
     */
    protected LogStore logStore;

    /**
     * The Log query limit.
     */
    protected int logQueryLimit;

    //endregion

    //region Init

    protected void initLogQueryLimit(String logQueryLimitConfigKey) {
        this.logQueryLimit = CONFIG.getInt(logQueryLimitConfigKey, DEFAULT_LOG_QUERY_LIMIT);
    }

    //endregion

    //region Override TransactionStoreManager

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return logStore.insertGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return logStore.updateGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return logStore.deleteGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return logStore.insertBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return logStore.updateBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return logStore.deleteBranchTransactionDO(convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    @Override
    public GlobalSession getSession(String xid, boolean withBranchSessions) {
        //global transaction
        GlobalTransactionDO globalTransactionDO = logStore.getGlobalTransactionDO(xid);
        //branch transactions
        return this.loadBranchs(globalTransactionDO, withBranchSessions);
    }

    @Override
    public List<GlobalSession> findSession(SessionCondition sessionCondition, boolean withBranchSessions) {
        if (sessionCondition.getLimit() <= 0) {
            sessionCondition.setLimit(logQueryLimit <= 0 ? DEFAULT_LOG_QUERY_LIMIT : logQueryLimit);
        }

        //global transactions
        List<GlobalTransactionDO> globalTransactionDOs = logStore.findGlobalTransactionDO(sessionCondition);
        //branch transactions
        return this.loadBranchs(globalTransactionDOs, withBranchSessions);
    }

    @Override
    public void shutdown() {
    }

    //endregion

    //region Load branch list

    protected GlobalSession loadBranchs(GlobalTransactionDO globalTransactionDO, boolean withBranchSessions) {
        if (globalTransactionDO == null) {
            return null;
        }
        List<BranchTransactionDO> branchTransactionDOs = null;
        if (withBranchSessions) {
            branchTransactionDOs = logStore.findBranchTransactionDO(globalTransactionDO.getXid());
        }
        return getGlobalSession(globalTransactionDO, branchTransactionDOs);
    }

    protected List<GlobalSession> loadBranchs(List<GlobalTransactionDO> globalTransactionDOs, boolean withBranchSessions) {
        if (CollectionUtils.isEmpty(globalTransactionDOs)) {
            return new ArrayList<>();
        }

        Map<String, List<BranchTransactionDO>> branchTransactionDOsMap;
        if (withBranchSessions) {
            List<String> xids = globalTransactionDOs.stream().map(GlobalTransactionDO::getXid).collect(Collectors.toList());
            List<BranchTransactionDO> branchTransactionDOs = logStore.findBranchTransactionDO(xids);
            branchTransactionDOsMap = branchTransactionDOs.stream()
                    .collect(Collectors.groupingBy(BranchTransactionDO::getXid, LinkedHashMap::new, Collectors.toList()));
        } else {
            branchTransactionDOsMap = null;
        }

        return globalTransactionDOs.stream().map(globalTransactionDO ->
                this.getGlobalSession(globalTransactionDO, branchTransactionDOsMap == null ? null
                        : branchTransactionDOsMap.get(globalTransactionDO.getXid())))
                .collect(Collectors.toList());
    }

    //endregion

    //region Converter

    protected GlobalTransactionDO convertGlobalTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof GlobalSession)) {
            throw new IllegalArgumentException(
                    "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        GlobalSession globalSession = (GlobalSession) session;

        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(globalSession.getXid());
        globalTransactionDO.setStatus(globalSession.getStatus() == null ? 0 : globalSession.getStatus().getCode());
        globalTransactionDO.setApplicationId(globalSession.getApplicationId());
        globalTransactionDO.setBeginTime(globalSession.getBeginTime());
        globalTransactionDO.setTimeout(globalSession.getTimeout());
        globalTransactionDO.setTransactionId(globalSession.getTransactionId());
        globalTransactionDO.setTransactionName(globalSession.getTransactionName());
        globalTransactionDO.setTransactionServiceGroup(globalSession.getTransactionServiceGroup());
        globalTransactionDO.setApplicationData(globalSession.getApplicationData());
        return globalTransactionDO;
    }

    protected BranchTransactionDO convertBranchTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof BranchSession)) {
            throw new IllegalArgumentException(
                    "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        BranchSession branchSession = (BranchSession) session;

        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid(branchSession.getXid());
        branchTransactionDO.setBranchId(branchSession.getBranchId());
        branchTransactionDO.setBranchType(branchSession.getBranchType() == null ? null : branchSession.getBranchType().name());
        branchTransactionDO.setClientId(branchSession.getClientId());
        branchTransactionDO.setResourceGroupId(branchSession.getResourceGroupId());
        branchTransactionDO.setTransactionId(branchSession.getTransactionId());
        branchTransactionDO.setApplicationData(branchSession.getApplicationData());
        branchTransactionDO.setResourceId(branchSession.getResourceId());
        branchTransactionDO.setStatus(branchSession.getStatus() == null ? 0 : branchSession.getStatus().getCode());
        return branchTransactionDO;
    }

    protected GlobalSession convertGlobalSession(GlobalTransactionDO globalTransactionDO) {
        GlobalSession session = new GlobalSession(globalTransactionDO.getApplicationId(),
                globalTransactionDO.getTransactionServiceGroup(), globalTransactionDO.getTransactionName(),
                globalTransactionDO.getTimeout());
        session.setTransactionId(globalTransactionDO.getTransactionId());
        session.setXid(globalTransactionDO.getXid());
        session.setStatus(GlobalStatus.get(globalTransactionDO.getStatus()));
        session.setApplicationData(globalTransactionDO.getApplicationData());
        session.setBeginTime(globalTransactionDO.getBeginTime());
        return session;
    }

    protected BranchSession convertBranchSession(BranchTransactionDO branchTransactionDO) {
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
        return branchSession;
    }

    protected GlobalSession getGlobalSession(GlobalTransactionDO globalTransactionDO,
                                             List<BranchTransactionDO> branchTransactionDOs) {
        GlobalSession globalSession = convertGlobalSession(globalTransactionDO);
        //branch transactions
        if (CollectionUtils.isNotEmpty(branchTransactionDOs)) {
            for (BranchTransactionDO branchTransactionDO : branchTransactionDOs) {
                globalSession.add(convertBranchSession(branchTransactionDO));
            }
        }
        return globalSession;
    }

    //endregion

    //region Gets and Sets

    @Override
    public void setLogStore(LogStore logStore) {
        this.logStore = logStore;
    }

    /**
     * Sets log query limit.
     *
     * @param logQueryLimit the log query limit
     */
    public void setLogQueryLimit(int logQueryLimit) {
        this.logQueryLimit = logQueryLimit;
    }

    //endregion
}
