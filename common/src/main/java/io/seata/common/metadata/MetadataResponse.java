package io.seata.common.metadata;

import java.util.List;

public class MetadataResponse {

    List<Node> nodes;

    String mode;

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override public String toString() {
        return "MetadataResponse{" + "nodes=" + nodes + ", mode='" + mode + '\'' + '}';
    }
}
