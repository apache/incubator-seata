package org.apache.seata.core.rpc.netty.grpc;

public enum GrpcHeaderEnum {

    /**
     * grpc状态
     */
    GRPC_STATUS("grpc-status"),
    /**
     * http2状态
     */
    HTTP2_STATUS(":status"),
    /**
     * content-type
     */
    GRPC_CONTENT_TYPE("content-type");

    public final String header;

    GrpcHeaderEnum(String header) {
        this.header = header;
    }
}
