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

import java.sql.SQLException;
import java.util.concurrent.Callable;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.exec.LockConflictException;
import io.seata.rm.datasource.exec.LockRetryController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author funkye
 */
public class JedisProxy extends Jedis {

    private final JedisContext context = new JedisContext();

    private Jedis jedis;

    public JedisProxy(Jedis jedis) {
        this.jedis = jedis;
        context.bind(RootContext.getXID());
    }

    /**
     * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1 GB).
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param value
     * @return Status code reply
     */
    @Override
    public String set(final String key, final String value) {
        context.appendLockKey(key);
        KVUndolog kvUndolog = new KVUndolog(key, jedis.get(key), value, KVUndolog.RedisMethod.set.method);
        context.appendUndoItem(kvUndolog);
        try {
            doRetryOnLockConflict(() -> DefaultResourceManager.get().branchRegister(BranchType.ATbyJedis,
                ((JedisPoolProxy)dataSource).getResourceId(), null, context.getXid(), context.getApplicationData(),
                context.buildLockKeys()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        checkIsInMultiOrPipeline();
        client.set(key, value);
        return client.getStatusCodeReply();
    }

    /**
     * Get the value of the specified key. If the key does not exist null is returned. If the value
     * stored at key is not a string an error is returned because GET can only handle string values.
     * <p>
     * Time complexity: O(1)
     * @param key
     * @return Bulk reply
     */
    @Override
    public String get(final String key) {
        context.appendLockKey(key);
        try {
            doRetryOnLockConflict(() -> {
                checkLock(key);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        checkIsInMultiOrPipeline();
        client.get(key);
        return client.getBulkReply();
    }


    @Override
    public Long hset(final String key, final String field, final String value) {
        context.appendLockKey(buildLockKey(key, field));
        KVUndolog kvUndolog = new KVUndolog(key, jedis.hget(key, field), value, KVUndolog.RedisMethod.hset.method);
        context.appendUndoItem(kvUndolog);
        try {
            doRetryOnLockConflict(() -> DefaultResourceManager.get().branchRegister(BranchType.ATbyJedis,
                ((JedisPoolProxy)dataSource).getResourceId(), null, context.getXid(), context.getApplicationData(),
                context.buildLockKeys()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        checkIsInMultiOrPipeline();
        client.hset(key, field, value);
        return client.getIntegerReply();
    }

    public Jedis getJedis() {
        return jedis;
    }

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    /**
     * Gets context.
     *
     * @return the context
     */
    public JedisContext getContext() {
        return context;
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    public void bind(String xid) {
        context.bind(xid);
    }

    /**
     * get global lock requires flag
     *
     * @return the boolean
     */
    public boolean isGlobalLockRequire() {
        return context.isGlobalLockRequire();
    }

    /**
     * set global lock requires flag
     *
     * @param isLock whether to lock
     */
    public void setGlobalLockRequire(boolean isLock) {
        context.setGlobalLockRequire(isLock);
    }

    /**
     * Check lock.
     *
     * @param lockKeys the lockKeys
     * @throws SQLException the sql exception
     */
    public void checkLock(String lockKeys) throws Exception {
        if (StringUtils.isBlank(lockKeys)) {
            return;
        }
        // Just check lock without requiring lock by now.
        try {
            boolean lockable = DefaultResourceManager.get().lockQuery(BranchType.ATbyJedis,
                ((JedisPoolProxy)dataSource).getResourceId(), context.getXid(), lockKeys);
            if (!lockable) {
                throw new LockConflictException(String.format("get lock failed, lockKey: %s", lockKeys));
            }
        } catch (TransactionException e) {
            recognizeLockKeyConflictException(e, lockKeys);
        }
    }

    /**
     * Lock query.
     *
     * @param lockKeys the lock keys
     * @return the boolean
     * @throws Exception the sql exception
     */
    public boolean lockQuery(String lockKeys) throws Exception {
        // Just check lock without requiring lock by now.
        boolean result = false;
        try {
            result = DefaultResourceManager.get().lockQuery(BranchType.ATbyJedis, ((JedisPoolProxy)dataSource).getResourceId(),
                context.getXid(), lockKeys);
        } catch (TransactionException e) {
            recognizeLockKeyConflictException(e, lockKeys);
        }
        return result;
    }

    private void recognizeLockKeyConflictException(TransactionException te) throws Exception {
        recognizeLockKeyConflictException(te, null);
    }

    private void recognizeLockKeyConflictException(TransactionException te, String lockKeys) throws Exception {
        if (te.getCode() == TransactionExceptionCode.LockKeyConflict
            || te.getCode() == TransactionExceptionCode.LockKeyConflictFailFast) {
            StringBuilder reasonBuilder = new StringBuilder("get global lock fail, xid:");
            reasonBuilder.append(context.getXid());
            if (StringUtils.isNotBlank(lockKeys)) {
                reasonBuilder.append(", lockKeys:").append(lockKeys);
            }
            throw new LockConflictException(reasonBuilder.toString(), te.getCode());
        } else {
            throw new JedisException(te);
        }

    }

    protected <T> T doRetryOnLockConflict(Callable<T> callable) throws Exception {
        LockRetryController lockRetryController = new LockRetryController();
        while (true) {
            try {
                return callable.call();
            } catch (LockConflictException lockConflict) {
                onException(lockConflict);
                lockRetryController.sleep(lockConflict);
            } catch (Exception e) {
                onException(e);
                throw e;
            }
        }
    }

    /**
     * Callback on exception in doLockRetryOnConflict.
     *
     * @param e invocation exception
     * @throws Exception error
     */
    protected void onException(Exception e) throws Exception {}

    private String buildLockKey(String key, String field) {
        if (field == null) {
            return key;
        }
        return key + "~~~~~>>>>>" + field;
    }
    
}
