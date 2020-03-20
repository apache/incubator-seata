package io.seata.server.store.redis;

import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.ReloadableStore;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.store.TransactionWriteStore;

import java.util.List;

public class RedisTransactionStoreManager extends AbstractTransactionStoreManager
    implements TransactionStoreManager, ReloadableStore {

    @Override public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        return false;
    }

    @Override public long getCurrentMaxSessionId() {
        return 0;
    }

    @Override public List<TransactionWriteStore> readWriteStore(int readSize, boolean isHistory) {
        return null;
    }

    @Override public boolean hasRemaining(boolean isHistory) {
        return false;
    }
}
