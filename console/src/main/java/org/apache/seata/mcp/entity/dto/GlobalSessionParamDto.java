package org.apache.seata.mcp.entity.dto;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.mcp.annotation.ToolParam;

import java.io.Serializable;

public class GlobalSessionParamDto implements Serializable {

    private static final long serialVersionUID = 115488252809011284L;
    /**
     * the xid
     */
    @ToolParam(description = "GLOBAL TRANSACTIONS id")
    private String xid;
    /**
     * the application id
     */
    @ToolParam(description = "applicationId")
    private String applicationId;
    /**
     * the global session status
     */
    @ToolParam(
            description = "the state enumeration class is in example",
            exampleValueClassName = {GlobalStatus.class, BranchStatus.class})
    private Integer status;
    /**
     * the transaction name
     */
    @ToolParam(description = "The name of the transaction")
    private String transactionName;

    /**
     * the vgroup
     */
    @ToolParam(description = "Belong to the group")
    private String vgroup;

    /**
     * if with branch
     * true: with branch session
     * false: no branch session
     */
    @ToolParam(description = "Whether or not it contains branch transaction information")
    private boolean withBranch;

    @ToolParam(description = "PAGE NUMBER", required = true, example = "1")
    private int pageNum;

    @ToolParam(description = "PageSize", required = true, example = "100")
    private int pageSize;

    @ToolParam(description = "The transaction start time is after this time (yyyy-MM-dd HH:mm:ss), Do not specify a time to query transactions for the last hour by default")
    private String timeStart;

    @ToolParam(description = "The transaction start time is before this time (yyyy-MM-dd HH:mm:ss), Do not specify a time to query transactions for the last hour by default")
    private String timeEnd;

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public boolean isWithBranch() {
        return withBranch;
    }

    public void setWithBranch(boolean withBranch) {
        this.withBranch = withBranch;
    }

    public String getVgroup() {
        return vgroup;
    }

    public void setVgroup(String vgroup) {
        this.vgroup = vgroup;
    }

    @Override
    public String toString() {
        return "GlobalSessionParamDto{" +
                "xid='" + xid + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", status=" + status +
                ", transactionName='" + transactionName + '\'' +
                ", vgroup='" + vgroup + '\'' +
                ", withBranch=" + withBranch +
                ", pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", timeStart='" + timeStart + '\'' +
                ", timeEnd='" + timeEnd + '\'' +
                '}';
    }
}
