package io.seata.core.rpc;

/**
 * @author goodboycoder
 */
public enum RpcType {
    /**
     * netty
     */
    NETTY("netty"),
    /**
     * grpc
     */
    GRPC("grpc");

    public final String name;

    RpcType(String name) {
        this.name = name;
    }
}
