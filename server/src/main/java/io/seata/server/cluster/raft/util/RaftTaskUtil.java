/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.cluster.raft.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.entity.Task;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.server.cluster.raft.RaftServerFactory;
import io.seata.server.cluster.raft.context.SeataClusterContext;
import io.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import io.seata.server.cluster.raft.sync.msg.RaftSyncMessage;
import io.seata.server.cluster.raft.sync.msg.RaftSessionSyncMsg;

/**
 * @author funkye
 */
public class RaftTaskUtil {

    public static boolean createTask(Closure done, Object data, CompletableFuture<Boolean> completableFuture)
        throws TransactionException {
        final Task task = new Task();
        if (data != null) {
            RaftSyncMessage raftSyncMessage = new RaftSyncMessage();
            raftSyncMessage.setBody(data);
            try {
                task.setData(ByteBuffer.wrap(RaftSyncMessageSerializer.encode(raftSyncMessage)));
            } catch (IOException e) {
                throw new TransactionException(e);
            }
        }
        task.setDone(done == null ? status -> {
        } : done);
        RaftServerFactory.getInstance().getRaftServer(SeataClusterContext.getGroup()).getNode().apply(task);
        if (completableFuture != null) {
            return futureGet(completableFuture);
        }
        return true;
    }

    public static boolean createTask(Closure done, CompletableFuture<Boolean> completableFuture)
        throws TransactionException {
        return createTask(done, null, completableFuture);
    }

    public static boolean futureGet(CompletableFuture<Boolean> completableFuture) throws TransactionException {
        try {
            return completableFuture.get();
        } catch (InterruptedException e) {
            throw new GlobalTransactionException(TransactionExceptionCode.FailedWriteSession,
                "Fail to store global session: " + e.getMessage());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TransactionException) {
                throw (TransactionException)e.getCause();
            } else {
                throw new GlobalTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Fail to store global session: " + e.getMessage());
            }
        }
    }

}
