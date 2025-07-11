package org.apache.seata.mcp.config;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * 服务器配置参数
 */
@Component
public class MCPConfiguration {


    /**
     * 服务器名称
     */
    private String serverName;
    /**
     * 服务器版本
     */
    private String serverVersion = "1.0.0";
    /**
     * sse端点
     */
    private String sseEndpoint = "/sse";
    /**
     * message端点
     */
    private String messageEndpoint = "/message";
    /**
     * 是否开启resource
     */
    private boolean resourceSupport = true;
    private boolean resourceTemplates = false;
    /**
     * 开启日志
     */
    private McpSchema.LoggingLevel loggingLevel = McpSchema.LoggingLevel.INFO;
    /**
     * 是否开启心跳监测，默认不开启
     */
    private boolean heartbeat = false;

    /**
     * 从环境中读取mcp相关配置参数
     */
    @Lazy
    @Autowired
    private Environment env;

    @PostConstruct
    public void init(){
        serverName = env.getProperty("seata.mcp.serverName","seata-mcp-server");
        serverVersion = env.getProperty("seata.mcp.serverVersion","1.0.0");
        sseEndpoint = env.getProperty("seata.mcp.sseEndpoint","/sse");
        messageEndpoint = env.getProperty("seata.mcp.messageEndpoint","/message");
        heartbeat = Boolean.parseBoolean(env.getProperty("seata.mcp.heartbeat","false"));
    }


    @Override
    public String toString() {
        return "MCPConfiguration{" +
                "serverName='" + serverName + '\'' +
                ", serverVersion='" + serverVersion + '\'' +
                ", sseEndpoint='" + sseEndpoint + '\'' +
                ", messageEndpoint='" + messageEndpoint + '\'' +
                ", resourceSupport=" + resourceSupport +
                ", resourceTemplates=" + resourceTemplates +
                ", loggingLevel=" + loggingLevel +
                ", heartbeat=" + heartbeat +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MCPConfiguration that = (MCPConfiguration) o;
        return resourceSupport == that.resourceSupport && resourceTemplates == that.resourceTemplates && heartbeat == that.heartbeat && Objects.equals(serverName, that.serverName) && Objects.equals(serverVersion, that.serverVersion) && Objects.equals(sseEndpoint, that.sseEndpoint) && Objects.equals(messageEndpoint, that.messageEndpoint) && loggingLevel == that.loggingLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverName, serverVersion, sseEndpoint, messageEndpoint, resourceSupport, resourceTemplates, loggingLevel, heartbeat);
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
    }

    public String getMessageEndpoint() {
        return messageEndpoint;
    }

    public void setMessageEndpoint(String messageEndpoint) {
        this.messageEndpoint = messageEndpoint;
    }

    public boolean isResourceSupport() {
        return resourceSupport;
    }

    public void setResourceSupport(boolean resourceSupport) {
        this.resourceSupport = resourceSupport;
    }

    public boolean isResourceTemplates() {
        return resourceTemplates;
    }

    public void setResourceTemplates(boolean resourceTemplates) {
        this.resourceTemplates = resourceTemplates;
    }

    public McpSchema.LoggingLevel getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(McpSchema.LoggingLevel loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }
}
