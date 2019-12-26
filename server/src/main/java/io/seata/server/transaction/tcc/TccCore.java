package io.seata.server.transaction.tcc;

import io.seata.core.model.BranchType;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.server.coordinator.AbstractCore;

/**
 * Created by txg on 2019-12-26.
 */
public class TccCore extends AbstractCore {

    public TccCore(ServerMessageSender messageSender) {
        super(messageSender);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.TCC;
    }
}
