package org.apache.seata.rm.datasource.entity;


public class ConnectionPoolMetrics {

    private Integer currentConnections;

    private Integer idleConnections;

    private Integer activeConnections;

    public Integer getCurrentConnections() {
        return currentConnections;
    }

    public void setCurrentConnections(Integer currentConnections) {
        this.currentConnections = currentConnections;
    }

    public Integer getIdleConnections() {
        return idleConnections;
    }

    public void setIdleConnections(Integer idleConnections) {
        this.idleConnections = idleConnections;
    }

    public Integer getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(Integer activeConnections) {
        this.activeConnections = activeConnections;
    }


}
