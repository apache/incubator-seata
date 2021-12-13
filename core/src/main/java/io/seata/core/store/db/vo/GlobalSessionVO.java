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
package io.seata.core.store.db.vo;

import io.seata.core.constants.ServerTableColumnsName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Set;

/**
 * GlobalSessionVO
 * @author: zhongxiang.wang
 */
public class GlobalSessionVO {

    private String xid;

    private Long transactionId;

    private Integer status;

    private String applicationId;

    private String transactionServiceGroup;

    private String transactionName;

    private Long timeout;

    private Long beginTime;

    private String applicationData;

    private Date gmtCreate;

    private Date gmtModified;

    private Set<BranchSessionVO> branchSessionVOs;

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Long beginTime) {
        this.beginTime = beginTime;
    }

    public String getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public Set<BranchSessionVO> getBranchSessionVOs() {
        return branchSessionVOs;
    }

    public void setBranchSessionVOs(Set<BranchSessionVO> branchSessionVOs) {
        this.branchSessionVOs = branchSessionVOs;
    }

    public static GlobalSessionVO convert(ResultSet rs) throws SQLException {
        GlobalSessionVO globalSessionVO = new GlobalSessionVO();
        globalSessionVO.setXid(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_XID));
        globalSessionVO.setTransactionId(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID));
        globalSessionVO.setStatus(rs.getInt(ServerTableColumnsName.GLOBAL_TABLE_STATUS));
        globalSessionVO.setApplicationId(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID));
        globalSessionVO.setTransactionServiceGroup(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP));
        globalSessionVO.setTransactionName(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME));
        globalSessionVO.setTimeout(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT));
        globalSessionVO.setBeginTime(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME));
        globalSessionVO.setApplicationData(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA));
        globalSessionVO.setGmtCreate(rs.getDate(ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE));
        globalSessionVO.setGmtModified(rs.getDate(ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED));
        return globalSessionVO;
    }
}
