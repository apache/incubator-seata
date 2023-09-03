package io.seata.core.protocol.transaction;

import io.seata.core.protocol.MessageType;

public class BranchDeleteResponse extends AbstractTransactionResponse {
    @Override
    public short getTypeCode() {
        return MessageType.TYPE_BRANCH_DELETE_RESULT;
    }
}
