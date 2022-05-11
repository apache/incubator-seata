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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Optional;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import io.seata.common.XID;
import io.seata.common.exception.RedisException;
import io.seata.common.exception.StoreException;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.server.console.param.GlobalSessionParam;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import static io.seata.common.ConfigurationKeys.STORE_REDIS_QUERY_LIMIT;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_XID;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_XID;
import static io.seata.core.constants.RedisKeyConstants.DEFAULT_LOG_QUERY_LIMIT;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_STATUS;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_STATUS;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_GLOBAL_GMT_MODIFIED;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_GMT_MODIFIED;
import static io.seata.core.constants.RedisKeyConstants.REDIS_KEY_BRANCH_APPLICATION_DATA;

/**
 * The redis transaction store manager
 *
 * @author funkye
 * @author wangzhongxiang
 * @author doubleDimple
 */
public class RedisTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTransactionStoreManager.class);

    /**the prefix of the branch transactions*/
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
     * The constant CONFIG.
     */
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The Log query limit.
     */
    private int logQueryLimit;

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

    /**
     * init map to constructor
     */
    public RedisTransactionStoreManager() {
        super();
        initGlobalMap();
        initBranchMap();
        logQueryLimit = CONFIG.getInt(STORE_REDIS_QUERY_LIMIT, DEFAULT_LOG_QUERY_LIMIT);
        /**
         * redis mode: if DEFAULT_LOG_QUERY_LIMIT < STORE_REDIS_QUERY_LIMIT get DEFAULT_LOG_QUERY_LIMIT if
         * DEFAULT_LOG_QUERY_LIMIT >= STORE_REDIS_QUERY_LIMIT get STORE_REDIS_QUERY_LIMIT
         */
        if (logQueryLimit > DEFAULT_LOG_QUERY_LIMIT) {
            logQueryLimit = DEFAULT_LOG_QUERY_LIMIT;
        }
    }

    /**
     * Map for LogOperation Global Operation
     */
    public static volatile ImmutableMap<LogOperation, Function<GlobalTransactionDO, Boolean>> globalMap;

    /**
     * Map for LogOperation Branch Operation
     */
    public static volatile ImmutableMap<LogOperation, Function<BranchTransactionDO, Boolean>> branchMap;


    /**
     * init globalMap
     *
     * @return void
     */
    public void initGlobalMap() {
        if (CollectionUtils.isEmpty(branchMap)) {
            globalMap = ImmutableMap.<LogOperation, Function<GlobalTransactionDO, Boolean>>builder()
                    .put(LogOperation.GLOBAL_ADD, this::insertGlobalTransactionDO)
                    .put(LogOperation.GLOBAL_UPDATE, this::updateGlobalTransactionDO)
                    .put(LogOperation.GLOBAL_REMOVE, this::deleteGlobalTransactionDO)
                    .build();
        }
    }

    /**
     * init branchMap
     *
     * @return void
     */
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
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (globalMap.containsKey(logOperation) || branchMap.containsKey(logOperation)) {
            return globalMap.containsKey(logOperation) ?
                    globalMap.get(logOperation).apply(SessionConverter.convertGlobalTransactionDO(session)) :
                    branchMap.get(logOperation).apply(SessionConverter.convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    /**
     * Insert branch transaction
     * @param branchTransactionDO
     * @return the boolean
     */
    private boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        String branchListKey = buildBranchListKeyByXid(branchTransactionDO.getXid());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance(); Pipeline pipelined = jedis.pipelined()) {
            Date now = new Date();
            branchTransactionDO.setGmtCreate(now);
            branchTransactionDO.setGmtModified(now);
            pipelined.hmset(branchKey, BeanUtils.objectToMap(branchTransactionDO));
            pipelined.rpush(branchListKey, branchKey);
            pipelined.sync();
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    /**
     * Delete the branch transaction
     * @param branchTransactionDO
     * @return
     */
    private boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xid = jedis.hget(branchKey, REDIS_KEY_BRANCH_XID);
            if (StringUtils.isEmpty(xid)) {
                return true;
            }
            String branchListKey = buildBranchListKeyByXid(branchTransactionDO.getXid());
            try (Pipeline pipelined = jedis.pipelined()) {
                pipelined.lrem(branchListKey, 0, branchKey);
                pipelined.del(branchKey);
                pipelined.sync();
            }
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    /**
     * Update the branch transaction
     * @param branchTransactionDO
     * @return
     */
    private boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String branchKey = buildBranchKey(branchTransactionDO.getBranchId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String previousBranchStatus = jedis.hget(branchKey, REDIS_KEY_BRANCH_STATUS);
            if (StringUtils.isEmpty(previousBranchStatus)) {
                throw new StoreException("Branch transaction is not exist, update branch transaction failed.");
            }
            Map<String, String> map = new HashMap<>(3, 1);
            map.put(REDIS_KEY_BRANCH_STATUS, String.valueOf(branchTransactionDO.getStatus()));
            map.put(REDIS_KEY_BRANCH_GMT_MODIFIED, String.valueOf((new Date()).getTime()));
            if (StringUtils.isNotBlank(branchTransactionDO.getApplicationData())) {
                map.put(REDIS_KEY_BRANCH_APPLICATION_DATA, String.valueOf(branchTransactionDO.getApplicationData()));
            }
            jedis.hmset(branchKey, map);
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    /**
     * Insert the global transaction.
     * @param globalTransactionDO
     * @return
     */
    private boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance(); Pipeline pipelined = jedis.pipelined()) {
            Date now = new Date();
            globalTransactionDO.setGmtCreate(now);
            globalTransactionDO.setGmtModified(now);
            pipelined.hmset(globalKey, BeanUtils.objectToMap(globalTransactionDO));
            pipelined.rpush(buildGlobalStatus(globalTransactionDO.getStatus()), globalTransactionDO.getXid());
            pipelined.sync();
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    /**
     * Delete the global transaction.
     * It will operate two parts:
     *  1.delete the global session map
     *  2.remove the xid from the global status list
     * If the operate failed,the succeed operates will rollback
     * @param globalTransactionDO
     * @return
     */
    private boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xid = jedis.hget(globalKey, REDIS_KEY_GLOBAL_XID);
            if (StringUtils.isEmpty(xid)) {
                LOGGER.warn("Global transaction is not exist,xid = {}.Maybe has been deleted by another tc server",
                    globalTransactionDO.getXid());
                return true;
            }
            try (Pipeline pipelined = jedis.pipelined()) {
                pipelined.lrem(buildGlobalStatus(globalTransactionDO.getStatus()), 0, globalTransactionDO.getXid());
                pipelined.del(globalKey);
                pipelined.sync();
            }
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    /**
     * Update the global transaction.
     * It will update two parts:
     *  1.the global session map
     *  2.the global status list
     * If the update failed,the succeed operates will rollback
     * @param globalTransactionDO
     * @return
     */
    private boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String xid = globalTransactionDO.getXid();
        String globalKey = buildGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            // Defensive watch to prevent other TC server operating concurrently,Fail fast
            jedis.watch(globalKey);
            List<String> statusAndGmtModified = jedis.hmget(globalKey, REDIS_KEY_GLOBAL_STATUS, REDIS_KEY_GLOBAL_GMT_MODIFIED);
            String previousStatus = statusAndGmtModified.get(0);
            if (StringUtils.isEmpty(previousStatus)) {
                jedis.unwatch();
                throw new StoreException("Global transaction is not exist, update global transaction failed.");
            }
            if (previousStatus.equals(String.valueOf(globalTransactionDO.getStatus()))) {
                jedis.unwatch();
                return true;
            }

            String previousGmtModified = statusAndGmtModified.get(1);
            Transaction multi = jedis.multi();
            Map<String,String> map = new HashMap<>(2);
            map.put(REDIS_KEY_GLOBAL_STATUS,String.valueOf(globalTransactionDO.getStatus()));
            map.put(REDIS_KEY_GLOBAL_GMT_MODIFIED,String.valueOf((new Date()).getTime()));
            multi.hmset(globalKey,map);
            multi.lrem(buildGlobalStatus(Integer.valueOf(previousStatus)),0, xid);
            multi.rpush(buildGlobalStatus(globalTransactionDO.getStatus()), xid);
            List<Object> exec = multi.exec();
            if (CollectionUtils.isEmpty(exec)) {
                //The data has changed by another tc, so we still think the modification is successful.
                LOGGER.warn("The global transaction xid = {}, maybe changed by another TC. It does not affect the results",globalTransactionDO.getXid());
                return true;
            }
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
                        Map<String,String> mapPrevious = new HashMap<>(2,1);
                        mapPrevious.put(REDIS_KEY_GLOBAL_STATUS,previousStatus);
                        mapPrevious.put(REDIS_KEY_GLOBAL_GMT_MODIFIED,previousGmtModified);
                        Transaction multi2 = jedis.multi();
                        multi2.hmset(globalKey,mapPrevious);
                        multi2.exec();
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
            throw new RedisException(ex);
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
        String transactionId = String.valueOf(XID.getTransactionId(xid));
        String globalKey = buildGlobalKeyByTransactionId(transactionId);
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Map<String, String> map  = jedis.hgetAll(globalKey);
            if (CollectionUtils.isEmpty(map)) {
                return null;
            }
            GlobalTransactionDO globalTransactionDO = (GlobalTransactionDO)BeanUtils.mapToObject(map, GlobalTransactionDO.class);
            List<BranchTransactionDO> branchTransactionDOs = null;
            if (withBranchSessions) {
                branchTransactionDOs = this.readBranchSessionByXid(jedis, xid);
            }
            GlobalSession session = getGlobalSession(globalTransactionDO, branchTransactionDOs, withBranchSessions);
            return session;
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
    @Override
    public List<GlobalSession> readSession(GlobalStatus[] statuses, boolean withBranchSessions) {

        List<GlobalSession> globalSessions = Collections.synchronizedList(new ArrayList<>());
        List<String> statusKeys = convertStatusKeys(statuses);

        Map<String, Integer> targetMap = calculateStatuskeysHasData(statusKeys);
        if (targetMap.size() == 0 || logQueryLimit <= 0) {
            return globalSessions;
        }
        int perStatusLimit = resetLogQueryLimit(targetMap);
        final long countGlobalSessions = targetMap.values().stream().collect(Collectors.summarizingInt(Integer::intValue)).getSum();
        // queryCount
        final long queryCount = Math.min(logQueryLimit, countGlobalSessions);
        List<List<String>> list = new ArrayList<>();
        dogetXidsForTargetMapRecursive(targetMap, 0L, perStatusLimit - 1, queryCount, list);
        if (CollectionUtils.isNotEmpty(list)) {
            List<String> xids = list.stream().flatMap(Collection::stream).collect(Collectors.toList());
            xids.parallelStream().forEach(xid -> {
                GlobalSession globalSession = this.readSession(xid, withBranchSessions);
                if (globalSession != null) {
                    globalSessions.add(globalSession);
                }
            });
        }
        return globalSessions;
    }

    /**
     * get everyone keys limit
     *
     * @param targetMap
     * @return
     */
    private int resetLogQueryLimit(Map<String, Integer> targetMap) {
        int resetLimitQuery = logQueryLimit;
        if (targetMap.size() > 1) {
            int size = targetMap.size();
            resetLimitQuery = (logQueryLimit / size) == 0 ? 1 : (logQueryLimit / size);
        }
        return resetLimitQuery;
    }

    /**
     * read the global session list by different condition
     * @param sessionCondition the session condition
     * @return the global sessions
     */
    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        List<GlobalSession> globalSessions = new ArrayList<>();
        if (StringUtils.isNotEmpty(sessionCondition.getXid())) {
            GlobalSession globalSession = this.readSession(sessionCondition.getXid(), !sessionCondition.isLazyLoadBranch());
            if (globalSession != null) {
                globalSessions.add(globalSession);
            }
            return globalSessions;
        } else if (sessionCondition.getTransactionId() != null) {
            GlobalSession globalSession = this
                .readSessionByTransactionId(sessionCondition.getTransactionId().toString(), !sessionCondition.isLazyLoadBranch());
            if (globalSession != null) {
                globalSessions.add(globalSession);
            }
            return globalSessions;
        } else if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readSession(sessionCondition.getStatuses(), !sessionCondition.isLazyLoadBranch());
        } else if (sessionCondition.getStatus() != null) {
            return readSession(new GlobalStatus[] {sessionCondition.getStatus()}, !sessionCondition.isLazyLoadBranch());
        }
        return null;
    }

    /**
     * query GlobalSession by status with page
     *
     * @param param
     * @return List<GlobalSession>
     */
    public List<GlobalSession> readSessionStatusByPage(GlobalSessionParam param) {
        List<GlobalSession> globalSessions = new ArrayList<>();

        int pageNum = param.getPageNum();
        int pageSize = param.getPageSize();
        int start = Math.max((pageNum - 1) * pageSize, 0);
        int end = pageNum * pageSize - 1;

        if (param.getStatus() != null) {
            String statusKey = buildGlobalStatus(GlobalStatus.get(param.getStatus()).getCode());
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                final List<String> xids = jedis.lrange(statusKey, start, end);
                xids.forEach(xid -> {
                    GlobalSession globalSession = this.readSession(xid, param.isWithBranch());
                    if (globalSession != null) {
                        globalSessions.add(globalSession);
                    }
                });
            }
        }
        return globalSessions;
    }

    /**
     * assemble the global session and branch session
     * @param globalTransactionDO the global transactionDo
     * @param branchTransactionDOs the branch transactionDos
     * @param withBranchSessions if read branch sessions
     * @return the global session with branch session
     */
    private GlobalSession getGlobalSession(GlobalTransactionDO globalTransactionDO,
        List<BranchTransactionDO> branchTransactionDOs, boolean withBranchSessions) {
        GlobalSession globalSession = SessionConverter.convertGlobalSession(globalTransactionDO, !withBranchSessions);
        if (CollectionUtils.isNotEmpty(branchTransactionDOs)) {
            for (BranchTransactionDO branchTransactionDO : branchTransactionDOs) {
                globalSession.add(SessionConverter.convertBranchSession(branchTransactionDO));
            }
        }
        return globalSession;
    }

    /**
     * read the global session by transactionId
     * @param transactionId the transaction id
     * @param withBranchSessions if read branch sessions
     * @return the global session
     */
    private GlobalSession readSessionByTransactionId(String transactionId, boolean withBranchSessions) {
        String globalKey = buildGlobalKeyByTransactionId(transactionId);
        String xid = null;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Map<String, String> map = jedis.hgetAll(globalKey);
            if (CollectionUtils.isEmpty(map)) {
                return null;
            }
            GlobalTransactionDO globalTransactionDO = (GlobalTransactionDO)BeanUtils.mapToObject(map, GlobalTransactionDO.class);
            if (globalTransactionDO != null) {
                xid = globalTransactionDO.getXid();
            }
            List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
            if (withBranchSessions) {
                branchTransactionDOs = this.readBranchSessionByXid(jedis, xid);
            }
            return getGlobalSession(globalTransactionDO, branchTransactionDOs, withBranchSessions);
        }
    }


    /**
     * Read the branch session list by xid
     * @param jedis the jedis
     * @param xid the xid
     * @return the branch transactionDo list
     */
    private List<BranchTransactionDO> readBranchSessionByXid(Jedis jedis, String xid) {
        List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
        String branchListKey = buildBranchListKeyByXid(xid);
        List<String> branchKeys = lRange(jedis, branchListKey);
        if (CollectionUtils.isNotEmpty(branchKeys)) {
            try (Pipeline pipeline = jedis.pipelined()) {
                branchKeys.stream().forEach(branchKey -> pipeline.hgetAll(branchKey));
                List<Object> branchInfos = pipeline.syncAndReturnAll();
                for (Object branchInfo : branchInfos) {
                    if (branchInfo != null) {
                        Map<String, String> branchInfoMap = (Map<String, String>)branchInfo;
                        Optional<BranchTransactionDO> branchTransactionDO = Optional.ofNullable(
                            (BranchTransactionDO)BeanUtils.mapToObject(branchInfoMap, BranchTransactionDO.class));
                        branchTransactionDO.ifPresent(branchTransactionDOs::add);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(branchTransactionDOs)) {
            Collections.sort(branchTransactionDOs);
        }
        return branchTransactionDOs;
    }

    private List<String> lRange(Jedis jedis, String key) {
        List<String> keys = new ArrayList<>();
        List<String> values;
        int limit = 20;
        int start = 0;
        int stop = limit;
        for (;;) {
            values = jedis.lrange(key, start, stop);
            keys.addAll(values);
            if (CollectionUtils.isEmpty(values) || values.size() < limit) {
                break;
            }
            start = keys.size();
            stop = start + limit;
        }
        return keys;
    }

    public List<BranchTransactionDO> findBranchSessionByXid(String xid) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            return readBranchSessionByXid(jedis, xid);
        }
    }

    /**
     * query globalSession by page
     *
     * @param pageNum
     * @param pageSize
     * @param withBranchSessions
     * @return List<GlobalSession>
     */
    public List<GlobalSession> findGlobalSessionByPage(int pageNum, int pageSize, boolean withBranchSessions) {
        List<GlobalSession> globalSessions = new ArrayList<>();
        int start = Math.max((pageNum - 1) * pageSize, 0);
        int end = pageNum * pageSize - 1;

        List<String> statusKeys = convertStatusKeys(GlobalStatus.values());
        Map<String, Integer> stringLongMap = calculateStatuskeysHasData(statusKeys);

        List<List<String>> list = dogetXidsForTargetMap(stringLongMap, start, end, pageSize);

        if (CollectionUtils.isNotEmpty(list)) {
            List<String> xids = list.stream().flatMap(Collection::stream).collect(Collectors.toList());
            xids.forEach(xid -> {
                if (globalSessions.size() < pageSize) {
                    GlobalSession globalSession = this.readSession(xid, withBranchSessions);
                    if (globalSession != null) {
                        globalSessions.add(globalSession);
                    }
                }
            });
        }
        return globalSessions;
    }

    /**
     * query and sort existing values
     *
     * @param statusKeys
     * @return
     */
    private Map<String, Integer> calculateStatuskeysHasData(List<String> statusKeys) {
        Map<String, Integer> resultMap = Collections.synchronizedMap(new LinkedHashMap<>());
        Map<String, Integer> keysMap = new HashMap<>(statusKeys.size());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance(); Pipeline pipelined = jedis.pipelined()) {
            statusKeys.forEach(key -> pipelined.llen(key));
            List<Long> counts = (List) pipelined.syncAndReturnAll();
            for (int i = 0; i < counts.size(); i++) {
                if (counts.get(i) > 0) {
                    keysMap.put(statusKeys.get(i), counts.get(i).intValue());
                }
            }
        }

        //sort
        List<Map.Entry<String, Integer>> list = new ArrayList<>(keysMap.entrySet());
        list.sort((o1, o2) -> o2.getValue() - o1.getValue());
        list.forEach(e -> resultMap.put(e.getKey(), e.getValue()));

        return resultMap;
    }

    /**
     * count GlobalSession total by status
     *
     * @param values
     * @return Long
     */
    public Long countByGlobalSessions(GlobalStatus[] values) {
        List<String> statusKeys = new ArrayList<>();
        Long total = 0L;
        for (GlobalStatus status : values) {
            statusKeys.add(buildGlobalStatus(status.getCode()));
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance(); Pipeline pipelined = jedis.pipelined()) {
            statusKeys.stream().forEach(statusKey -> pipelined.llen(statusKey));
            List<Long> list = (List<Long>)(List)pipelined.syncAndReturnAll();
            if (list.size() > 0) {
                total = list.stream().mapToLong(value -> value).sum();
            }
            return total;
        }
    }

    private List<String> convertStatusKeys(GlobalStatus[] statuses) {
        List<String> statusKeys = new ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            statusKeys.add(buildGlobalStatus(statuses[i].getCode()));
        }
        return statusKeys;
    }

    private void dogetXidsForTargetMapRecursive(Map<String, Integer> targetMap, long start, long end,
                                                long queryCount, List<List<String>> listList) {

        long total = listList.stream().mapToLong(List::size).sum();

        if (total >= queryCount) {
            return;
        }
        // when start greater than offset(totalCount)
        if (start >= queryCount) {
            return;
        }

        if (targetMap.size() == 0) {
            return;
        }

        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            for (String key : targetMap.keySet()) {
                final long sum = listList.stream().mapToLong(List::size).sum();
                final long diffCount = queryCount - sum;
                if (diffCount <= 0) {
                    return;
                }
                List<String> list;
                if (end - start >= diffCount) {
                    long endNew = start + diffCount - 1;
                    list = jedis.lrange(key, start, endNew);
                } else {
                    list = jedis.lrange(key, start, end);
                }

                if (list.size() > 0 && sum < queryCount) {
                    listList.add(list);
                } else {
                    if (list.size() == 0) {
                        targetMap.remove(key);
                    }
                }
            }
        }
        long startNew = end + 1;
        long endNew = startNew + end - start;
        dogetXidsForTargetMapRecursive(targetMap, startNew, endNew, queryCount, listList);
    }

    private List<List<String>> dogetXidsForTargetMap(Map<String, Integer> targetMap, int start, int end,
                                                     int totalCount) {
        List<List<String>> listList = new ArrayList<>();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            for (String key : targetMap.keySet()) {
                final List<String> list = jedis.lrange(key, start, end);
                final long sum = listList.stream().mapToLong(List::size).sum();
                if (list.size() > 0 && sum < totalCount) {
                    listList.add(list);
                } else {
                    start = 0;
                    end = totalCount - 1;
                }
            }
        }
        return listList;
    }

    private String buildBranchListKeyByXid(String xid) {
        return REDIS_SEATA_BRANCHES_PREFIX + xid;
    }

    private String buildGlobalKeyByTransactionId(Object transactionId) {
        return REDIS_SEATA_GLOBAL_PREFIX + transactionId;
    }

    private String buildBranchKey(Long branchId) {
        return REDIS_SEATA_BRANCH_PREFIX + branchId;
    }

    private String buildGlobalStatus(Integer status) {
        return REDIS_SEATA_STATUS_PREFIX + status;
    }

    /**
     * Sets log query limit.
     *
     * @param logQueryLimit the log query limit
     */
    public void setLogQueryLimit(int logQueryLimit) {
        this.logQueryLimit = logQueryLimit;
    }

}
