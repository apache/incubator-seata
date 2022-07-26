package io.seata.server.console.param;

import io.seata.console.param.BaseParam;

import java.io.Serializable;

/**
 * @author TheR1sing3un
 * @date 2022/7/26 22:03
 * @description
 */

public class ClientOfflineParam extends BaseParam implements Serializable {
    private static final long serialVersionUID = 195593845420020908L;

    // app:ip:port
    private String clientId;

    // resourceId, for Rm
    private String resourceId;

    // TMROLE/RMROLE
    private String clientRole;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getClientRole() {
        return clientRole;
    }

    public void setClientRole(String clientRole) {
        this.clientRole = clientRole;
    }
}
