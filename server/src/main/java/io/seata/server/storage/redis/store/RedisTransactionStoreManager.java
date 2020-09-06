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
package io.seata.server.storage.redis.store;

import io.seata.common.util.BeanUtils;
import io.seata.server.storage.SessionConverter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_APPLICATION_DATA;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_BRANCH_ID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_BRANCH_TYPE;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_CLIENT_ID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_GMT_CREATE;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_GMT_MODIFIED;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_RESOURCE_GROUP_ID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_RESOURCE_ID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_STATUS;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_TRANSACTION_ID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_XID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_APPLICATION_DATA;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_APPLICATION_ID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_BEGIN_TIME;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_GMT_CREATE;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_GMT_MODIFIED;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_STATUS;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_TIMEOUT;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_TRANSACTION_ID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_TRANSACTION_NAME;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_TRANSACTION_SERVICE_GROUP;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_XID;

/**
 * The redis transaction store manager
 *
 * @author funkye
 * @author wangzhongxiang 
 */
public class RedisTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTransactionStoreManager.class);

    /**the prefix of the branchs transaction*/
    private static final String REDIS_SEATA_BRANCHES_PREFIX = "SEATA_BRANCHES_";
    
    /**the prefix of the branch transaction*/
    private static final String REDIS_SEATA_BRANCH_PREFIX = "SEATA_BRANCH_";

    /**the prefix of the global transaction*/
    private static final String REDIS_SEATA_GLOBAL_PREFIX = "SEATA_GLOBAL_";

    /**the prefix of the global transaction status*/
    private static final String REDIS_SEATA_STATUS_PREFIX = "SEATA_STATUS_";

    private static volatile RedisTransactionStoreManager instance;

    private static final String OK = "OK";

    /**
     * Get the instance.
     */
    public static RedisTransactionStoreManager getInstance() {
        if (instance == null) {
            synchronized (RedisTransactionStoreManager.class) {
                if (instance == null) {
                    instance = new RedisTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return insertGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return updateGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return deleteGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return insertBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return updateBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return deleteBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    /**
     * Insert branch transaction info
     * @param branchTransactionDO
     * @return the boolean
     */
    private boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        String branchListKey = buildBranchListKeyByXid(branchTransactionDO.getXid());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String hmset = jedis.hmset(branchKey, branchTransactionDOToMap(branchTransactionDO));
            if (OK.equalsIgnoreCase(hmset)) {
                Long rpush = jedis.rpush(branchListKey, branchKey);
                if (rpush > 0) {
                    return true;
                } else {
                    jedis.del(branchKey);
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception ex) {
            throw new StoreException(ex);
        }
    }

    private boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xid = jedis.hget(branchKey, REDIS_KEY_BRANCH_XID);
            if (StringUtils.isEmpty(xid)) {
                return true;
            }
            String branchListKey = buildBranchListKeyByXid(branchTransactionDO.getXid());
            Long lrem = jedis.lrem(branchListKey, 0, branchKey);
            if (lrem > 0) {
                Long del = jedis.del(branchKey);
                if (del == 1) {
                    return true;
                } else {
                    jedis.rpush(branchListKey,branchKey);
                    return false;
                }
            }
            return false;
        } catch (Exception ex) {
            throw new StoreException(ex);
        }
    }

    private boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String previousBranchStatus = jedis.hget(branchKey, REDIS_KEY_BRANCH_STATUS);
            if (StringUtils.isEmpty(previousBranchStatus)) {
                throw new StoreException("Branch transaction is not exist, update branch transaction failed.");
            }
            Map<String,String> map = new HashMap<>(2);
            map.put(REDIS_KEY_BRANCH_STATUS,String.valueOf(branchTransactionDO.getStatus()));
            map.put(REDIS_KEY_BRANCH_GMT_MODIFIED,String.valueOf((new Date()).getTime()));
            String hmset = jedis.hmset(branchKey, map);
            if (OK.equalsIgnoreCase(hmset)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new StoreException(ex);
        }
    }

    private boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String hmset = jedis.hmset(globalKey, globalTransactionDOToMap(globalTransactionDO));
            if (OK.equalsIgnoreCase(hmset)) {
                Long rpush = jedis.rpush(buildGlobalStatus(globalTransactionDO.getStatus()),
                        globalTransactionDO.getXid());
                if (rpush > 0) {
                    return true;
                } else {
                    jedis.hdel(globalKey);
                    return false;
                }
            }
            return false;
        } catch (Exception ex) {
            throw new StoreException(ex);
        }
    }

    private boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xid = jedis.hget(globalKey, REDIS_KEY_GLOBAL_XID);
            if (StringUtils.isEmpty(xid)) {
                LOGGER.warn("Global transaction is not exist,xid = {}.Maybe has been deleted by another tc server",
                        globalTransactionDO.getXid());
                return true;
            }
            Long lrem = jedis.lrem(buildGlobalStatus(globalTransactionDO.getStatus()), 0,
                    globalTransactionDO.getXid());
            if (lrem > 0) {
                Long del = jedis.del(globalKey);
                if (del > 0) {
                    return true;
                } else {
                    jedis.rpush(buildGlobalStatus(globalTransactionDO.getStatus()),globalTransactionDO.getXid());
                    return false;
                }
            }
            return false;
        } catch (Exception ex) {
            throw new StoreException(ex);
        }
    }

    private boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String xid = globalTransactionDO.getXid();
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String previousStatus = jedis.hget(globalKey, REDIS_KEY_GLOBAL_STATUS);
            if (StringUtils.isEmpty(previousStatus)) {
                throw new StoreException("Global transaction is not exist, update global transaction failed.");
            }
            // Defensive watch to prevent other TC server operating concurrently,Fail fast
            jedis.watch(globalKey);
            String previousGmtModified = jedis.hget(globalKey, REDIS_KEY_GLOBAL_GMT_MODIFIED);
            Transaction multi = jedis.multi();
            Map<String,String> map = new HashMap<>(2);
            map.put(REDIS_KEY_GLOBAL_STATUS,String.valueOf(globalTransactionDO.getStatus()));
            map.put(REDIS_KEY_GLOBAL_GMT_MODIFIED,String.valueOf((new Date()).getTime()));
            multi.hmset(globalKey,map);
            multi.lrem(buildGlobalStatus(Integer.valueOf(previousStatus)),0, xid);
            multi.rpush(buildGlobalStatus(globalTransactionDO.getStatus()), xid);
            List<Object> exec = multi.exec();
            String hmset = exec.get(0).toString();
            long lrem  = (long)exec.get(1);
            long rpush = (long)exec.get(2);
            if (OK.equalsIgnoreCase(hmset) && lrem > 0 && rpush > 0) {
                return true;
            } else {
                // If someone failed, the succeed operations need rollback
                if (OK.equalsIgnoreCase(hmset)) {
                    // Defensive watch to prevent other TC server operating concurrently,give up this operate
                    jedis.watch(globalKey);
                    String xid2 = jedis.hget(globalKey, REDIS_KEY_GLOBAL_XID);
                    if (StringUtils.isNotEmpty(xid2)) {
                        Map<String,String> mapPrevious = new HashMap<>(2);
                        mapPrevious.put(REDIS_KEY_GLOBAL_STATUS,previousStatus);
                        mapPrevious.put(REDIS_KEY_GLOBAL_GMT_MODIFIED,previousGmtModified);
                        jedis.multi();
                        multi.hmset(globalKey,mapPrevious);
                        multi.exec();
                    }
                }
                if (lrem > 0) {
                    jedis.rpush(buildGlobalStatus(Integer.valueOf(previousStatus)),xid);
                }
                if (rpush > 0) {
                    jedis.lrem(buildGlobalStatus(globalTransactionDO.getStatus()),0,xid);
                }
                return false;
            }
        } catch (Exception ex) {
            throw new StoreException(ex);
        }
    }

    /**
     * Read session global session.
     *
     * @param xid the xid
     * @param withBranchSessions  the withBranchSessions
     * @return the global session
     */
    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        String transactionId = xid.split(":")[2];
        String globalKey = buildGlobalKeyByTransactionId(transactionId);
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Map<String, String> map  = jedis.hgetAll(globalKey);
            if (CollectionUtils.isEmpty(map)) {
                return null;
            }
            GlobalTransactionDO globalTransactionDO = (GlobalTransactionDO)BeanUtils.mapToObject(map, GlobalTransactionDO.class);
            List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
            if (withBranchSessions) {
                branchTransactionDOs = this.readBranchSessionByXid(jedis,xid);
            }
            return getGlobalSession(globalTransactionDO,branchTransactionDOs);
        }
    }

    /**
     * Read session global session.
     *
     * @param xid
     *            the xid
     * @return the global session
     */
    @Override
    public GlobalSession readSession(String xid) {
        return this.readSession(xid, true);
    }

    /**
     * Read globalSession list by global status
     *
     * @param statuses the statuses
     * @return the list
     */
    public List<GlobalSession> readSession(GlobalStatus[] statuses) {
        List<String> statusKeys = new ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            statusKeys.add(buildGlobalStatus(statuses[i].getCode()));
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Pipeline pipelined = jedis.pipelined();
            statusKeys.stream().forEach(statusKey -> pipelined.lrange(statusKey,0,-1));
            List<List<String>> list = (List<List<String>>)(List)pipelined.syncAndReturnAll();
            List<String> xids = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(list)) {
                xids = list.stream().flatMap(ll -> ll.stream()).collect(Collectors.toList());
            }
            List<GlobalSession> globalSessions = new ArrayList<>();
            xids.parallelStream().forEach(xid -> globalSessions.add(this.readSession(xid,true)));
            return globalSessions;
        }
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        List<GlobalSession> globalSessions = new ArrayList<>();
        if (StringUtils.isNotEmpty(sessionCondition.getXid())) {
            globalSessions.add(this.readSession(sessionCondition.getXid(), true));
            return globalSessions;
        } else if (sessionCondition.getTransactionId() != null) {
            globalSessions.add(this.readSessionByTransactionId(sessionCondition.getTransactionId().toString(),true));
            return globalSessions;
        } else if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readSession(sessionCondition.getStatuses());
        } else if (sessionCondition.getStatus() != null) {
            return readSession(new GlobalStatus[]{sessionCondition.getStatus()});
        }
        return null;
    }

    private GlobalSession getGlobalSession(GlobalTransactionDO globalTransactionDO,
        List<BranchTransactionDO> branchTransactionDOs) {
        GlobalSession globalSession = SessionConverter.convertGlobalSession(globalTransactionDO);
        if (CollectionUtils.isNotEmpty(branchTransactionDOs)) {
            for (BranchTransactionDO branchTransactionDO : branchTransactionDOs) {
                globalSession.add(SessionConverter.convertBranchSession(branchTransactionDO));
            }
        }
        return globalSession;
    }

    private GlobalSession readSessionByTransactionId(String transactionId, boolean withBranchSessions) {
        String globalKey = buildGlobalKeyByTransactionId(transactionId);
        String xid = null;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Map<String, String> map  = jedis.hgetAll(globalKey);
            if (CollectionUtils.isEmpty(map)) {
                return null;
            }
            GlobalTransactionDO globalTransactionDO = (GlobalTransactionDO)BeanUtils.mapToObject(map, GlobalTransactionDO.class);
            if (globalTransactionDO != null) {
                xid = globalTransactionDO.getXid();
            }
            List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
            if (withBranchSessions) {
                branchTransactionDOs = this.readBranchSessionByXid(jedis,xid);
            }
            return getGlobalSession(globalTransactionDO,branchTransactionDOs);
        }
    }

    private List<BranchTransactionDO> readBranchSessionByXid(Jedis jedis,String xid) {
        List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
        String branchListKey = buildBranchListKeyByXid(xid);
        List<String> branchKeys = jedis.lrange(branchListKey, 0, -1);
        Pipeline pipeline = jedis.pipelined();
        if (CollectionUtils.isNotEmpty(branchKeys)) {
            branchKeys.stream().forEachOrdered(branchKey -> pipeline.hgetAll(branchKey));
            List<Object> branchInfos = pipeline.syncAndReturnAll();
            for (Object branchInfo : branchInfos) {
                if (branchInfo != null) {
                    Map<String, String> branchInfoMap = (Map<String, String>) branchInfo;
                    BranchTransactionDO branchTransactionDO =
                            (BranchTransactionDO) BeanUtils.mapToObject(branchInfoMap, BranchTransactionDO.class);
                    branchTransactionDOs.add(branchTransactionDO);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(branchTransactionDOs)) {
            branchTransactionDOs = branchTransactionDOs.stream().sorted(Comparator.comparing(BranchTransactionDO::getGmtCreate))
                    .collect(Collectors.toList());
        }
        return branchTransactionDOs;
    }


    private Map<String,String> branchTransactionDOToMap(BranchTransactionDO branchTransactionDO) {
        Map<String,String> map = new HashMap<>(16);
        map.put(REDIS_KEY_BRANCH_XID,branchTransactionDO.getXid());
        map.put(REDIS_KEY_BRANCH_TRANSACTION_ID,String.valueOf(branchTransactionDO.getTransactionId()));
        map.put(REDIS_KEY_BRANCH_BRANCH_ID,String.valueOf(branchTransactionDO.getBranchId()));
        map.put(REDIS_KEY_BRANCH_RESOURCE_GROUP_ID,
                StringUtils.isEmpty(branchTransactionDO.getResourceGroupId()) ? "" : branchTransactionDO.getResourceGroupId());
        map.put(REDIS_KEY_BRANCH_RESOURCE_ID,branchTransactionDO.getResourceId());
        map.put(REDIS_KEY_BRANCH_BRANCH_TYPE,branchTransactionDO.getBranchType());
        map.put(REDIS_KEY_BRANCH_STATUS,String.valueOf(branchTransactionDO.getStatus()));
        map.put(REDIS_KEY_BRANCH_CLIENT_ID,branchTransactionDO.getClientId());
        map.put(REDIS_KEY_BRANCH_GMT_CREATE,String.valueOf((new Date()).getTime()));
        map.put(REDIS_KEY_BRANCH_GMT_MODIFIED,String.valueOf((new Date()).getTime()));
        map.put(REDIS_KEY_BRANCH_APPLICATION_DATA,
                StringUtils.isEmpty(branchTransactionDO.getApplicationData()) ? "" :
                        branchTransactionDO.getApplicationData());
        return map;
    }

    private Map<String,String> globalTransactionDOToMap(GlobalTransactionDO globalTransactionDO) {
        Map<String,String> map = new HashMap<>(16);
        map.put(REDIS_KEY_GLOBAL_XID,globalTransactionDO.getXid());
        map.put(REDIS_KEY_GLOBAL_TRANSACTION_ID,String.valueOf(globalTransactionDO.getTransactionId()));
        map.put(REDIS_KEY_GLOBAL_STATUS,String.valueOf(globalTransactionDO.getStatus()));
        map.put(REDIS_KEY_GLOBAL_APPLICATION_ID,globalTransactionDO.getApplicationId());
        map.put(REDIS_KEY_GLOBAL_TRANSACTION_SERVICE_GROUP,globalTransactionDO.getTransactionServiceGroup());
        map.put(REDIS_KEY_GLOBAL_TRANSACTION_NAME,globalTransactionDO.getTransactionName());
        map.put(REDIS_KEY_GLOBAL_TIMEOUT,String.valueOf(globalTransactionDO.getTimeout()));
        map.put(REDIS_KEY_GLOBAL_BEGIN_TIME,String.valueOf(globalTransactionDO.getBeginTime()));
        map.put(REDIS_KEY_GLOBAL_GMT_CREATE,String.valueOf((new Date()).getTime()));
        map.put(REDIS_KEY_GLOBAL_GMT_MODIFIED,String.valueOf((new Date()).getTime()));
        map.put(REDIS_KEY_GLOBAL_APPLICATION_DATA,
                StringUtils.isEmpty(globalTransactionDO.getApplicationData()) ? "" :
                        globalTransactionDO.getApplicationData());
        return map;
    }

    private String buildBranchListKeyByXid(String xid) {
        return REDIS_SEATA_BRANCHES_PREFIX + xid;
    }

    private String buildGlobalKeyByTransactionId(Long transactionId) {
        return REDIS_SEATA_GLOBAL_PREFIX + transactionId;
    }
    private String buildGlobalKeyByTransactionId(String transactionId) {
        return REDIS_SEATA_GLOBAL_PREFIX + transactionId;
    }

    private String buildBranchKey(Long branchId) {
        return REDIS_SEATA_BRANCH_PREFIX + branchId;
    }

    private String buildGlobalStatus(Integer status) {
        return REDIS_SEATA_STATUS_PREFIX + status;
    }
}
