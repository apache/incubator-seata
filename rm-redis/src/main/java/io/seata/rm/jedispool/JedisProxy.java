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

import java.util.concurrent.Callable;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.exec.LockConflictException;
import io.seata.rm.datasource.exec.LockRetryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author funkye
 */
public class JedisProxy extends Jedis {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JedisProxy.class);

    private final JedisContext context = new JedisContext();

    private Jedis jedis;
    
    private Long branchId;

    public JedisProxy(Jedis jedis, JedisPoolAbstract dataSource) {
        this.jedis = jedis;
        context.bind(RootContext.getXID());
        this.dataSource = dataSource;
    }

    /**
     * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1 GB).
     * <p>
     * Time complexity: O(1)
     * 
     * @param key key
     * @param value value
     * @return Status code reply
     */
    @Override
    public String set(final String key, final String value) {
        registry(key, null, value, KVUndolog.RedisMethod.set.method);
        return jedis.set(key, value);
    }

    /**
     * SETNX works exactly like {@link #set(String, String) SET} with the only difference that if the
     * key already exists no operation is performed. SETNX actually means "SET if Not eXists".
     * <p>
     * Time complexity: O(1)
     * @param key key
     * @param value value
     * @return Integer reply, specifically: 1 if the key was set 0 if the key was not set
     */
    @Override
    public Long setnx(final String key, final String value) {
        registry(key, null, value, KVUndolog.RedisMethod.setnx.method);
        long result = jedis.setnx(key, value);
        try {
            return result;
        } finally {
            if (result != 1) {
                try {
                    // set nx In the event of a failure, you do not need to issue a two-stage directive because the operation was not committed
                    DefaultResourceManager.get().branchReport(BranchType.ATbyRedis, RootContext.getXID(), branchId,
                        BranchStatus.PhaseOne_Failed, null);
                } catch (TransactionException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Get the value of the specified key. If the key does not exist null is returned. If the value
     * stored at key is not a string an error is returned because GET can only handle string values.
     * <p>
     * Time complexity: O(1)
     * @param key key
     * @return Bulk reply
     */
    @Override
    public String get(final String key) {
        readCommitted(key);
        return jedis.get(key);
    }


    @Override
    public Long hset(final String key, final String field, final String value) {
        registry(key, field, value, KVUndolog.RedisMethod.hset.method);
        return jedis.hset(key, field, value);
    }

    /**
     * If key holds a hash, retrieve the value associated to the specified field.
     * <p>
     * If the field is not found or the key does not exist, a special 'nil' value is returned.
     * <p>
     * <b>Time complexity:</b> O(1)
     * @param key key
     * @param field field
     * @return Bulk reply
     */
    @Override
    public String hget(final String key, final String field) {
        readCommitted(key, field);
        return jedis.hget(key, field);
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
     * registry branch
     * @param key key
     * @param value value
     */
    public void registry(String key, String field, String value, String method) {
        if (StringUtils.isNotBlank(value)) {
            context.appendLockKey(buildLockKey(key, field));
            KVUndolog kvUndolog = new KVUndolog(key, field,
                StringUtils.isNotBlank(field) ? jedis.hget(key, field) : jedis.get(key), value, method);
            context.appendUndoItem(kvUndolog);
            try {
                doRetryOnLockConflict(() -> branchId = DefaultResourceManager.get().branchRegister(BranchType.ATbyRedis,
                    ((JedisPoolProxy)dataSource).getResourceId(), null, context.getXid(), context.getApplicationData(),
                    context.buildLockKeys()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                context.reset();
            }
        }
    }

    /**
     * read committed.
     *
     * @param key the key
     */
    public void readCommitted(String key) {
        readCommitted(key, null);
    }

    /**
     * read committed.
     *
     * @param key the key
     * @param field the field
     */
    public void readCommitted(String key, String field) {
        context.appendLockKey(buildLockKey(key, field));
        try {
            doRetryOnLockConflict(() -> {
                checkLock(context.buildLockKeys());
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            context.reset();
        }
    }

    public void checkLock(String lockKeys) throws Exception {
        if (StringUtils.isBlank(lockKeys)) {
            return;
        }
        // Just check lock without requiring lock by now.
        try {
            boolean lockable = DefaultResourceManager.get().lockQuery(BranchType.ATbyRedis,
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
            result = DefaultResourceManager.get().lockQuery(BranchType.ATbyRedis, ((JedisPoolProxy)dataSource).getResourceId(),
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
            return getDB() + ":" + key;
        }
        return getDB() + ":" + key + "~~~~~>>>>>" + field;
    }

    private String buildLockKey(String key) {
        return buildLockKey(key, null);
    }

}
