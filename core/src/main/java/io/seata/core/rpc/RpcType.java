package io.seata.core.rpc;

public enum RpcType {
    /**
     * Netty (base on TCP)
     */
    NETTY("Netty"),

    /**
     * Grpc (base on HTTP2)
     */
    GRPC("Grpc");

    public final String name;

    RpcType(String name) {
        this.name = name;
    }
}
