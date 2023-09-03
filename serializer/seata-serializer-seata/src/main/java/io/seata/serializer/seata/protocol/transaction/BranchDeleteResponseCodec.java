package io.seata.serializer.seata.protocol.transaction;

import io.seata.core.protocol.transaction.BranchDeleteResponse;

import java.io.Serializable;

public class BranchDeleteResponseCodec extends AbstractTransactionResponseCodec implements Serializable {
    @Override
    public Class<?> getMessageClassType() {
        return BranchDeleteResponse.class;
    }
}
