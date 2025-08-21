package org.apache.seata.mcp.entity.param;

import org.apache.seata.mcp.annotation.ToolParam;

public class GlobalAbnormalSessionParam {
    @ToolParam(description = "Whether or not it contains branch transaction information, default is true")
    private boolean withBranch = true;

    @ToolParam(description = "Start Time (Timestamp)")
    private Long timeStart;

    @ToolParam(description = "End Time (Timestamp)")
    private Long timeEnd;

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

    public Long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Long timeEnd) {
        this.timeEnd = timeEnd;
    }
}
