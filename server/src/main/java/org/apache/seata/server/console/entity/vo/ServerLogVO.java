package org.apache.seata.server.console.entity.vo;

import java.util.List;

public class ServerLogVO {

    private Integer cursor;

    private Long costedTime;

    private List<String> logMessages;

    public ServerLogVO (Integer cursor, Long costedTime, List<String> logMessages){
        this.cursor = cursor;
        this.costedTime = costedTime;
        this.logMessages = logMessages;
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

    public List<String> getLogs() {
        return logMessages;
    }

    public void setLogs(List<String> logMessages) {
        this.logMessages = logMessages;
    }

    @Override
    public String toString() {
        return "ServerLogVO{" +
                "cursor=" + cursor +
                ", costedTime=" + costedTime +
                ", logs=" + logMessages +
                '}';
    }
}
