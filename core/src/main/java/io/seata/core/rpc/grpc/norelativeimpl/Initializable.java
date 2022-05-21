package io.seata.core.rpc.grpc.norelativeimpl;

public interface Initializable {
    void initialize(String applicationId, String transactionServiceGroup);
}
