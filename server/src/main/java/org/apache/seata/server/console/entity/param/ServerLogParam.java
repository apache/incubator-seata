package org.apache.seata.server.console.entity.param;

import java.io.Serializable;

public class ServerLogParam implements Serializable {

    private static final long serialVersionUID = 225478653801012285L;

    private Integer cursor;

    private Integer nextLines;

    private Long costedTime;

    private String logType;

    private String logTime;

    public Integer getNextLines() {
        return nextLines;
    }

    public void setNextLines(Integer nextLines) {
        this.nextLines = nextLines;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getLogTime() {
        return logTime;
    }

    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    public Integer getCursor() {
        return cursor;
    }

    public void setCursor(Integer cursor) {
        this.cursor = cursor;
    }

    public Long getCostedTime() {
        return costedTime;
    }

    public void setCostedTime(Long costedTime) {
        this.costedTime = costedTime;
    }

    @Override
    public String toString() {
        return "ServerLogParam{" +
                "cursor=" + cursor +
                ", nextLines=" + nextLines +
                ", costedTime=" + costedTime +
                ", logType='" + logType + '\'' +
                ", logTime='" + logTime + '\'' +
                '}';
    }
}
