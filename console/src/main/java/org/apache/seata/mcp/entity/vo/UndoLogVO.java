package org.apache.seata.mcp.entity.vo;

import org.apache.seata.mcp.undo.parser.UndoLogParser;
import org.apache.seata.mcp.utils.DateUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UndoLogVO {
    private String rollBackInfo;
    private String context;
    private Integer logStatus;
    private String logCreated;
    private String logModified;

    public String getRollBackInfo() {
        return rollBackInfo;
    }

    public void setRollBackInfo(String rollBackInfo) {
        this.rollBackInfo = rollBackInfo;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Integer getLogStatus() {
        return logStatus;
    }

    public void setLogStatus(Integer logStatus) {
        this.logStatus = logStatus;
    }

    public String getLogCreated() {
        return logCreated;
    }

    public void setLogCreated(String logCreated) {
        this.logCreated = logCreated;
    }

    public String getLogModified() {
        return logModified;
    }

    public void setLogModified(String logModified) {
        this.logModified = logModified;
    }

    @Override
    public String toString() {
        return "UndoLogVO{" + "rollBackInfo='"
                + rollBackInfo + '\'' + ", context='"
                + context + '\'' + ", logStatus="
                + logStatus + ", logCreated='"
                + logCreated + '\'' + ", logModified='"
                + logModified + '\'' + '}';
    }

    public static UndoLogVO convert(ResultSet rs) throws SQLException {
        UndoLogVO vo = new UndoLogVO();
        UndoLogParser parser = new UndoLogParser();
        vo.setContext(rs.getString("context"));
        vo.setLogCreated(DateUtils.convertToDateTimeFromTimestamp(
                rs.getTimestamp("log_created").getTime()));
        vo.setLogModified(DateUtils.convertToDateTimeFromTimestamp(
                rs.getTimestamp("log_modified").getTime()));
        vo.setLogStatus(rs.getInt("log_status"));
        vo.setRollBackInfo(parser.decode(rs.getBytes("rollback_info")));
        return vo;
    }
}
