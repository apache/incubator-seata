package org.apache.seata.mcp.entity.param;

import org.apache.seata.mcp.annotation.ToolParam;

public class GlobalAbnormalSessionParam {
    @ToolParam(description = "Whether or not it contains branch transaction information, default is true")
    private boolean withBranch = true;

    @ToolParam(description = "Start Time, DateTime format (yyyy-MM-dd HH:mm:ss)")
    private String timeStart;

    @ToolParam(description = "End Time, DateTime format (yyyy-MM-dd HH:mm:ss)")
    private String timeEnd;

    @Override
    public String toString() {
        return "GlobalAbnormalSessionParam{" +
                "withBranch=" + withBranch +
                ", timeStart=" + timeStart +
                ", timeEnd=" + timeEnd +
                '}';
    }

    public boolean isWithBranch() {
        return withBranch;
    }

    public void setWithBranch(boolean withBranch) {
        this.withBranch = withBranch;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }
}
