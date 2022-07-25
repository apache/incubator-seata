package io.seata.server.console.param;

import io.seata.console.param.BaseParam;

import java.io.Serializable;

public class ClientQueryParam extends BaseParam implements Serializable {

    private static final long serialVersionUID = 195593845420021030L;

    private String ip;

    private String applicationId;

    private String clientRole;

    private String resourceId;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getClientRole() {
        return clientRole;
    }

    public void setClientRole(String clientRole) {
        this.clientRole = clientRole;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "ClientQueryParam{" +
                "ip='" + ip + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", clientRole='" + clientRole + '\'' +
                ", resourceId='" + resourceId + '\'' +
                '}';
    }
}
