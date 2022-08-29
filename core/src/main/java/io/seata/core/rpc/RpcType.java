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

    /**
     * Gets rpc type by name.
     *
     * @param name the name
     * @return the type by name
     */
    public static RpcType getTypeByName(String name) {
        for (RpcType rpcType : values()) {
            if (rpcType.name().equalsIgnoreCase(name)) {
                return rpcType;
            }
        }
        throw new IllegalArgumentException("unknown rpc type:" + name);
    }
}
