/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.storage.redis.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.seata.common.exception.RedisException;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.BeanUtils;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.apache.seata.server.storage.redis.LuaParser;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import static org.apache.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_APPLICATION_DATA;
import static org.apache.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_GMT_MODIFIED;
import static org.apache.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_STATUS;
import static org.apache.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_XID;
import static org.apache.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_GMT_MODIFIED;
import static org.apache.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_STATUS;
import static org.apache.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_XID;

/**
 */
public class RedisLuaTransactionStoreManager extends RedisTransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLuaTransactionStoreManager.class);

    private static final String LUA_PREFIX = "lua/redisStore/";

    private static final String INSERT_TRANSACTION_DO_LUA_FILE_NAME = LUA_PREFIX + "insertTransactionDO.lua";

    private static final String DELETE_TRANSACTION_DO_LUA_FILE_NAME = LUA_PREFIX + "deleteTransactionDO.lua";

    private static final String UPDATE_BRANCH_TRANSACTION_DO_LUA_FILE_NAME = LUA_PREFIX + "updateBranchTransactionDO.lua";

    private static final String UPDATE_GLOBAL_TRANSACTION_DO_LUA_FILE_NAME = LUA_PREFIX + "updateGlobalTransactionDO.lua";

    private static final String ROLLBACK_GLOBAL_TRANSACTION_DO_LUA_FILE_NAME = LUA_PREFIX + "rollbackGlobalTransactionDO.lua";

    /**
     * key filename
     * value LOCK_SHA_SCRIPT_ID
     */
    private static final Map<String, String> LOCK_SHA_MAP = new HashMap<>();

    /**
     * load redis lua script
     */
    private void initRedisMode() {
        loadLuaFile(INSERT_TRANSACTION_DO_LUA_FILE_NAME, "insertTransactionDO");
        loadLuaFile(DELETE_TRANSACTION_DO_LUA_FILE_NAME, "deleteTransactionDO");
        loadLuaFile(UPDATE_BRANCH_TRANSACTION_DO_LUA_FILE_NAME, "updateBranchTransactionDO");
        loadLuaFile(UPDATE_GLOBAL_TRANSACTION_DO_LUA_FILE_NAME, "updateGlobalTransactionDO");
        loadLuaFile(ROLLBACK_GLOBAL_TRANSACTION_DO_LUA_FILE_NAME, "rollbackGlobalTransactionDO");
    }

    private void loadLuaFile(String fileName, String mode) {
        try {
            LOCK_SHA_MAP.putAll(LuaParser.getEvalShaMapFromFile(fileName));
        } catch (IOException e) {
            // if it fails to read the file, pipeline mode is used
            if (LOCK_SHA_MAP.get(fileName) != null) {
                LOCK_SHA_MAP.remove(fileName);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("redis session: {} use pipeline mode", mode);
            }
        }
    }

    public RedisLuaTransactionStoreManager() {
        LOGGER.info("init redisLuaTransactionStoreManager");
        initRedisMode();
    }

    @Override
    public void initGlobalMap() {
        if (CollectionUtils.isEmpty(branchMap)) {
            globalMap = ImmutableMap.<LogOperation, Function<GlobalTransactionDO, Boolean>>builder()
                .put(LogOperation.GLOBAL_ADD, this::insertGlobalTransactionDO)
                .put(LogOperation.GLOBAL_UPDATE, this::updateGlobalTransactionDO)
                .put(LogOperation.GLOBAL_REMOVE, this::deleteGlobalTransactionDO)
                .build();
        }
    }

    @Override
    public void initBranchMap() {
        if (CollectionUtils.isEmpty(branchMap)) {
            branchMap = ImmutableMap.<LogOperation, Function<BranchTransactionDO, Boolean>>builder()
                .put(LogOperation.BRANCH_ADD, this::insertBranchTransactionDO)
                .put(LogOperation.BRANCH_UPDATE, this::updateBranchTransactionDO)
                .put(LogOperation.BRANCH_REMOVE, this::deleteBranchTransactionDO)
                .build();
        }
    }

    @Override
    protected boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        String branchListKey = buildBranchListKeyByXid(branchTransactionDO.getXid());
        Date now = new Date();
        branchTransactionDO.setGmtCreate(now);
        branchTransactionDO.setGmtModified(now);
        Map<String, String> branchTransactionDOMap = BeanUtils.objectToMap(branchTransactionDO);
        String luaSHA = LOCK_SHA_MAP.get(INSERT_TRANSACTION_DO_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.insertBranchTransactionDO(branchTransactionDO);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<String> keys = new ArrayList<String>() {
                {
                    add(branchKey);
                    add(branchListKey);
                }
            };
            List<String> args = new ArrayList<String>() {
                {
                    add("branch");
                    add(String.valueOf(branchTransactionDOMap.size()));
                }
            };
            for (Map.Entry<String, String> entry : branchTransactionDOMap.entrySet()) {
                keys.add(entry.getKey());
                args.add(entry.getValue());
            }
            LuaParser.jedisEvalSha(jedis, luaSHA, INSERT_TRANSACTION_DO_LUA_FILE_NAME, keys, args);
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    protected boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        String branchListKey = buildBranchListKeyByXid(branchTransactionDO.getXid());
        String luaSHA = LOCK_SHA_MAP.get(DELETE_TRANSACTION_DO_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.deleteBranchTransactionDO(branchTransactionDO);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<String> keys = new ArrayList<String>() {
                {
                    add(branchKey);
                    add(branchListKey);
                    add(REDIS_KEY_BRANCH_XID);
                }
            };
            List<String> args = new ArrayList<String>() {
                {
                    add("branch");
                }
            };
            LuaParser.jedisEvalSha(jedis, luaSHA, DELETE_TRANSACTION_DO_LUA_FILE_NAME, keys, args);
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    protected boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        String branchStatus = String.valueOf(branchTransactionDO.getStatus());
        String applicationData = String.valueOf(branchTransactionDO.getApplicationData());
        String luaSHA = LOCK_SHA_MAP.get(UPDATE_BRANCH_TRANSACTION_DO_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.updateBranchTransactionDO(branchTransactionDO);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<String> keys = new ArrayList<String>() {
                {
                    add(branchKey);
                    add(REDIS_KEY_BRANCH_STATUS);
                    add(REDIS_KEY_BRANCH_GMT_MODIFIED);
                    add(REDIS_KEY_BRANCH_APPLICATION_DATA);
                }
            };
            List<String> args = new ArrayList<String>() {
                {
                    add(branchStatus);
                    add(String.valueOf((new Date()).getTime()));
                    add(applicationData);
                }
            };
            String result = (String)LuaParser.jedisEvalSha(jedis, luaSHA, UPDATE_BRANCH_TRANSACTION_DO_LUA_FILE_NAME, keys, args);
            LuaParser.LuaResult luaResult = LuaParser.getObjectFromJson(result, LuaParser.LuaResult.class);
            if (!luaResult.getSuccess()) {
                throw new StoreException("Branch transaction is not exist, update branch transaction failed.");
            } else {
                return true;
            }
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    protected boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        String globalStatus = buildGlobalStatus(globalTransactionDO.getStatus());
        String xid = globalTransactionDO.getXid();
        Date now = new Date();
        globalTransactionDO.setGmtCreate(now);
        globalTransactionDO.setGmtModified(now);
        Map<String, String> globalTransactionDOMap = BeanUtils.objectToMap(globalTransactionDO);
        String luaSHA = LOCK_SHA_MAP.get(INSERT_TRANSACTION_DO_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.insertGlobalTransactionDO(globalTransactionDO);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            // lua mode
            List<String> keys = new ArrayList<String>() {
                {
                    add(globalKey);
                    add(globalStatus);
                }
            };
            List<String> args = new ArrayList<String>() {
                {
                    add("global");
                    add(String.valueOf(globalTransactionDOMap.size()));
                }
            };
            for (Map.Entry<String, String> entry : globalTransactionDOMap.entrySet()) {
                keys.add(entry.getKey());
                args.add(entry.getValue());
            }
            keys.add(REDIS_SEATA_BEGIN_TRANSACTIONS_KEY);
            args.add(xid);
            args.add(String.valueOf(globalTransactionDO.getBeginTime() + globalTransactionDO.getTimeout()));
            LuaParser.jedisEvalSha(jedis, luaSHA, INSERT_TRANSACTION_DO_LUA_FILE_NAME, keys, args);
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    protected boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        String globalStatus = buildGlobalStatus(globalTransactionDO.getStatus());
        String luaSHA = LOCK_SHA_MAP.get(DELETE_TRANSACTION_DO_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.deleteGlobalTransactionDO(globalTransactionDO);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            // lua mode
            List<String> keys = new ArrayList<String>() {
                {
                    add(globalKey);
                    add(globalStatus);
                    add(REDIS_KEY_GLOBAL_XID);
                    add(REDIS_SEATA_BEGIN_TRANSACTIONS_KEY);
                }
            };
            List<String> args = new ArrayList<String>() {
                {
                    add("global");
                    add(globalTransactionDO.getXid());
                    add(String.valueOf(globalTransactionDO.getStatus()));
                }
            };
            LuaParser.jedisEvalSha(jedis, luaSHA, DELETE_TRANSACTION_DO_LUA_FILE_NAME, keys, args);
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    protected boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String xid = globalTransactionDO.getXid();
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        Integer status = globalTransactionDO.getStatus();

        String luaSHA = LOCK_SHA_MAP.get(UPDATE_GLOBAL_TRANSACTION_DO_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.updateGlobalTransactionDO(globalTransactionDO);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<String> keys = new ArrayList<String>() {
                {
                    add(globalKey);
                    add(REDIS_KEY_GLOBAL_STATUS);
                    add(REDIS_KEY_GLOBAL_GMT_MODIFIED);
                    add(REDIS_SEATA_BEGIN_TRANSACTIONS_KEY);
                }
            };
            List<String> args = new ArrayList<String>() {
                {
                    add(String.valueOf(status));
                    add(String.valueOf((new Date()).getTime()));
                    add(xid);
                }
            };
            String result = (String)LuaParser.jedisEvalSha(jedis, luaSHA, UPDATE_GLOBAL_TRANSACTION_DO_LUA_FILE_NAME, keys, args);
            LuaParser.LuaResult luaResult = LuaParser.getObjectFromJson(result, LuaParser.LuaResult.class);
            // fail
            if (!luaResult.getSuccess()) {
                String type = luaResult.getStatus();
                if (LuaParser.LuaErrorStatus.XID_NOT_EXISTED.equals(type)) {
                    throw new StoreException("Global transaction is not exist, update global transaction failed.");
                } else if (LuaParser.LuaErrorStatus.ILLEGAL_CHANGE_STATUS.equals(type)) {
                    String previousStatus = luaResult.getData();
                    GlobalStatus before = GlobalStatus.get(Integer.parseInt(previousStatus));
                    GlobalStatus after = GlobalStatus.get(status);
                    throw new StoreException("Illegal changing of global status, update global transaction failed."
                        + " beforeStatus[" + before.name() + "] cannot be changed to afterStatus[" + after.name() + "]");
                }
            }
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }
}
