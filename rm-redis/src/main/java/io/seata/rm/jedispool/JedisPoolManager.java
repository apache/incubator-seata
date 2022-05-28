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
package io.seata.rm.jedispool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.AbstractResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.context.RootContext;
import io.seata.core.exception.RmTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.logger.StackTraceLogger;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.rpc.netty.RmNettyRemotingClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * The type jedis pool manager.
 *
 * @author funkye
 */
public class JedisPoolManager extends AbstractResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JedisPoolManager.class);

    private final Map<String, Resource> jedisPoolCache = new ConcurrentHashMap<>();

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
        GlobalLockQueryRequest request = new GlobalLockQueryRequest();
        request.setXid(xid);
        request.setLockKey(lockKeys);
        request.setResourceId(resourceId);
        try {
            GlobalLockQueryResponse response;
            if (RootContext.inGlobalTransaction() || RootContext.requireGlobalLock()) {
                response = (GlobalLockQueryResponse) RmNettyRemotingClient.getInstance().sendSyncRequest(request);
            } else {
                throw new RuntimeException("unknow situation!");
            }

            if (response.getResultCode() == ResultCode.Failed) {
                throw new TransactionException(response.getTransactionExceptionCode(),
                    "Response[" + response.getMsg() + "]");
            }
            return response.isLockable();
        } catch (TimeoutException toe) {
            throw new RmTransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            throw new RmTransactionException(TransactionExceptionCode.LockableCheckFailed, "Runtime", rex);
        }
    }

    @Override
    public void registerResource(Resource resource) {
        JedisPoolProxy jedisPoolProxy = (JedisPoolProxy) resource;
        jedisPoolCache.put(jedisPoolProxy.getResourceId(), jedisPoolProxy);
        super.registerResource(jedisPoolProxy);
    }

    @Override
    public void unregisterResource(Resource resource) {
        throw new NotSupportYetException("unregister a resource");
    }

    /**
     * Get data source proxy.
     *
     * @param resourceId the resource id
     * @return the data source proxy
     */
    public JedisPoolProxy get(String resourceId) {
        return (JedisPoolProxy) jedisPoolCache.get(resourceId);
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
        String applicationData) throws TransactionException {
        JedisPoolProxy jedisPoolProxy = get(resourceId);
        if (jedisPoolProxy == null) {
            throw new ShouldNeverHappenException(String.format("resource: %s not found", resourceId));
        }
        JedisContext jedisContext = new JedisContext();
        jedisContext.reloadApplicationData(applicationData);
        if (CollectionUtils.isNotEmpty(jedisContext.getUndoItems())) {
            try (Jedis jedis = jedisPoolProxy.getResource()) {
                jedisContext.getUndoItems().parallelStream().forEach(undolog -> {
                    KVUndolog.RedisMethod redisMethod = KVUndolog.RedisMethod.valueOf(undolog.getMethod());
                    if (redisMethod == KVUndolog.RedisMethod.setnx) {
                        String currentValue = jedis.get(undolog.getKey());
                        if (StringUtils.equalsIgnoreCase(currentValue, undolog.getAfterValue())) {
                            jedis.set(undolog.getKey(), undolog.getAfterValue());
                        }
                    }
                });
            }
        }
        return BranchStatus.PhaseTwo_Committed;
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
        String applicationData) throws TransactionException {
        JedisPoolProxy jedisPoolProxy = get(resourceId);
        if (jedisPoolProxy == null) {
            throw new ShouldNeverHappenException(String.format("resource: %s not found", resourceId));
        }
        try {
            JedisContext jedisContext = new JedisContext();
            jedisContext.reloadApplicationData(applicationData);
            if (CollectionUtils.isNotEmpty(jedisContext.getUndoItems())) {
                try (Jedis jedis = jedisPoolProxy.getResource(); Pipeline pipeline = jedis.pipelined()) {
                    jedisContext.getUndoItems().parallelStream().forEach(undolog -> {
                        KVUndolog.RedisMethod redisMethod = KVUndolog.RedisMethod.valueOf(undolog.getMethod());
                        if (redisMethod == KVUndolog.RedisMethod.set) {
                            pipeline.set(undolog.getKey(), undolog.getBeforeValue());
                        }
                        if (redisMethod == KVUndolog.RedisMethod.hset) {
                            pipeline.hset(undolog.getKey(), undolog.getField(), undolog.getBeforeValue());
                        }
                        if (redisMethod == KVUndolog.RedisMethod.setnx) {
                            pipeline.set(undolog.getKey(), undolog.getBeforeValue());
                        }
                    });
                    pipeline.sync();
                }
            }
        } catch (TransactionException te) {
            StackTraceLogger.info(LOGGER, te,
                "branchRollback failed. branchType:[{}], xid:[{}], branchId:[{}], resourceId:[{}], applicationData:[{}]. reason:[{}]",
                new Object[] {branchType, xid, branchId, resourceId, applicationData, te.getMessage()});
            if (te.getCode() == TransactionExceptionCode.BranchRollbackFailed_Unretriable) {
                return BranchStatus.PhaseTwo_RollbackFailed_Unretryable;
            } else {
                return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
            }
        }
        return BranchStatus.PhaseTwo_Rollbacked;
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return jedisPoolCache;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.ATbyRedis;
    }

}
