package org.apache.seata.server.console.entity.param;

import java.io.Serializable;

public class ServerLogParam implements Serializable {

    private static final long serialVersionUID = 225478653801012285L;

    private String logType;

    private String logTime;

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

    @Override
    public String toString() {
        return "ServerLogParam{" +
                "logType='" + logType + '\'' +
                ", logTime='" + logTime + '\'' +
                '}';
    }
}
