package org.apache.seata.mcp.entity.dto;

import org.apache.seata.mcp.annotation.ToolParam;

import java.io.Serializable;

public class GlobalLockParamDto implements Serializable {

    private static final long serialVersionUID = 615412528070131284L;

    /**
     * the xid
     */
    @ToolParam(description = "Global transaction id")
    private String xid;
    /**
     * the table name
     */
    @ToolParam(description = "the table name")
    private String tableName;
    /**
     * the transaction id
     */
    @ToolParam(description = "the transaction id")
    private String transactionId;
    /**
     * the branch id
     */
    @ToolParam(description = "the branch id")
    private String branchId;
    /**
     * the primary Key
     */
    @ToolParam(description = "the primary Key")
    private String pk;
    /**
     * the resourceId
     */
    @ToolParam(description = "resourceId")
    private String resourceId;

    @ToolParam(description = "page number", required = true)
    private int pageNum;

    @ToolParam(description = "Page size", required = true)
    private int pageSize;

    @ToolParam(description = "Start time, The global lock create time is after this time (yyyy-MM-dd HH:mm:ss)")
    private String timeStart;

    @ToolParam(description = "End time, The global lock create time is before this time (yyyy-MM-dd HH:mm:ss)")
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "GlobalLockParam{" + "xid='"
                + xid + '\'' + ", tableName='"
                + tableName + '\'' + ", transactionId='"
                + transactionId + '\'' + ", branchId='"
                + branchId + '\'' + ", pk='"
                + pk + '\'' + ", resourceId='"
                + resourceId + '\'' + ", pageNum="
                + pageNum + ", pageSize="
                + pageSize + ", timeStart="
                + timeStart + ", timeEnd="
                + timeEnd + '}';
    }
}
