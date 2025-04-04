package org.apache.seata.rm.datasource.entity;


public class ConnectionPoolMetrics {

    private String resourceId;

    private Long currentTimeMillis;

    private Integer activeCount;

    private Integer idleCount;

    private Integer maxActive;

    public ConnectionPoolMetrics(){

    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(Integer activeCount) {
        this.activeCount = activeCount;
    }

    public Integer getIdleCount() {
        return idleCount;
    }

    public void setIdleCount(Integer idleCount) {
        this.idleCount = idleCount;
    }

    public Integer getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    public Long getCurrentTimeMillis() {
        return currentTimeMillis;
    }

    public void setCurrentTimeMillis(Long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }
}
