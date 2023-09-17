package io.seata.common.metadata;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class Instance {
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

    private static Instance instance;


    private Instance() {
    }

    public static Instance getInstance() {
        if (instance == null) {
            instance = new Instance();
        }
        return instance;
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

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>(16);
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
        Instance instance = (Instance) o;
        return Objects.equals(ip, instance.ip) && Objects.equals(nettyPort, instance.nettyPort)
                && Objects.equals(grpcPort, instance.grpcPort) && Objects.equals(port, instance.port)
                && Objects.equals(unit, instance.unit);
    }

    public String MapToJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof HashMap) {
                HashMap<String, Object> objectHashMap = (HashMap<String, Object>) entry.getValue();
                sb.append(MapToJsonString((objectHashMap)));
            } else if (entry.getValue() instanceof String) {
                sb.append("\"");
                sb.append(entry.getValue());
                sb.append("\"");
            } else {
                sb.append(entry.getValue());
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    // Recursively convert metadata to JSON
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"namespace\": \"").append(namespace).append("\", ");
        sb.append("\"clusterName\": \"").append(clusterName).append("\", ");
        sb.append("\"unit\": \"").append(unit).append("\", ");
        sb.append("\"ip\": \"").append(ip).append("\", ");
        sb.append("\"port\": ").append(port).append(", ");
        sb.append("\"weight\": ").append(weight).append(", ");
        sb.append("\"healthy\": ").append(healthy).append(", ");
        sb.append("\"timeStamp\": ").append(timeStamp).append(", ");
        sb.append("\"metadata\": ");

        // handle metadata kv map
        sb.append(MapToJsonString(metadata));

        sb.append("}");
        return sb.toString();
    }



}

