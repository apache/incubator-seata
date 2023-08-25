package io.seata.discovery.registry.namingserver;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class NamingInstance {
    private String namespace;
    private String clusterName;
    private String unit;
    private String ip;
    private int port;
    private int nettyPort;
    private int grpcPort;
    private double weight = 1.0;
    private boolean healthy = true;
    private long timeStamp;
    private String role;


    private Map<String, Object> metadata = new HashMap<>();


    public NamingInstance() {
    }


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
    public long getTimeStamp() {
        return timeStamp;
    }

    public int getNettyPort() {
        return nettyPort;
    }

    public void setNettyPort(int nettyPort) {
        this.nettyPort = nettyPort;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap(4);
        }

        this.metadata.put(key, value);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NamingInstance node = (NamingInstance) o;
        return Objects.equals(ip, node.ip) && Objects.equals(nettyPort, node.nettyPort)
                && Objects.equals(grpcPort, node.grpcPort) && Objects.equals(port, node.port)
                && Objects.equals(unit, node.unit);
    }

    // 将对象转换为JSON字符串
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"namespace\": \"").append(namespace).append("\", ");
        sb.append("\"clusterName\": \"").append(clusterName).append("\", ");
        sb.append("\"ip\": \"").append(ip).append("\", ");
        sb.append("\"port\": ").append(port).append(", ");
        sb.append("\"weight\": ").append(weight).append(", ");
        sb.append("\"healthy\": ").append(healthy).append(", ");
        sb.append("\"timeStamp\": ").append(timeStamp).append(", ");
        sb.append("\"metadata\": {");

        // 处理 metadata 的键值对
        int i = 0;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            i++;
        }

        sb.append("}}");
        return sb.toString();
    }


    // 从JSON字符串反序列化得到对象
    public static NamingInstance fromJsonString(String jsonString) {
        return null;
    }

}
