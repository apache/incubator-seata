package io.seata.server.storage.redis.store;

import com.alibaba.fastjson.JSON;

import io.seata.common.exception.StoreException;
import io.seata.common.util.StringUtils;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import redis.clients.jedis.Jedis;

/**
 * @author funkye
 * @date 2020/3/26
 */
public class RedisTransactionStoreManager  extends AbstractTransactionStoreManager
    implements TransactionStoreManager {

    private static String DEFAULT_REDIS_SEATA_GLOBAL_PREFIX = "SEATA_GLOBAL_";

    private static String DEFAULT_REDIS_SEATA_BRANCH_PREFIX = "SEATA_BRANCH_";

    private static volatile RedisTransactionStoreManager instance;

    /**
     * Get the instance.
     */
    public static RedisTransactionStoreManager getInstance() {
        if (null == instance) {
            synchronized (RedisTransactionStoreManager.class) {
                if (null == instance) {
                    instance = new RedisTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return insertOrUpdateGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return insertOrUpdateGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return deleteGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return insertOrUpdateBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return insertOrUpdateBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return deleteBranchTransactionDO(convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    private boolean deleteBranchTransactionDO(BranchTransactionDO convertBranchTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.del(DEFAULT_REDIS_SEATA_BRANCH_PREFIX + convertBranchTransactionDO.getXid()
                + convertBranchTransactionDO.getBranchId());
            return true;
        }
    }

    private boolean insertOrUpdateBranchTransactionDO(BranchTransactionDO convertBranchTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.set(DEFAULT_REDIS_SEATA_BRANCH_PREFIX + convertBranchTransactionDO.getXid()
                + convertBranchTransactionDO.getBranchId(), JSON.toJSONString(convertBranchTransactionDO));
            return true;
        }
    }

    private boolean deleteGlobalTransactionDO(GlobalTransactionDO convertGlobalTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.del(DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + convertGlobalTransactionDO.getXid());
            return true;
        }
    }

    private boolean insertOrUpdateGlobalTransactionDO(GlobalTransactionDO convertGlobalTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.set(DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + convertGlobalTransactionDO.getXid(),
                JSON.toJSONString(convertGlobalTransactionDO));
            return true;
        }
    }

    @Override
    public long getCurrentMaxSessionId() {
        return 0;
    }

    private GlobalTransactionDO convertGlobalTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof GlobalSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        GlobalSession globalSession = (GlobalSession)session;

        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(globalSession.getXid());
        globalTransactionDO.setStatus(globalSession.getStatus().getCode());
        globalTransactionDO.setApplicationId(globalSession.getApplicationId());
        globalTransactionDO.setBeginTime(globalSession.getBeginTime());
        globalTransactionDO.setTimeout(globalSession.getTimeout());
        globalTransactionDO.setTransactionId(globalSession.getTransactionId());
        globalTransactionDO.setTransactionName(globalSession.getTransactionName());
        globalTransactionDO.setTransactionServiceGroup(globalSession.getTransactionServiceGroup());
        globalTransactionDO.setApplicationData(globalSession.getApplicationData());
        return globalTransactionDO;
    }
    private BranchTransactionDO convertBranchTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof BranchSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        BranchSession branchSession = (BranchSession)session;

        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid(branchSession.getXid());
        branchTransactionDO.setBranchId(branchSession.getBranchId());
        branchTransactionDO.setBranchType(branchSession.getBranchType().name());
        branchTransactionDO.setClientId(branchSession.getClientId());
        branchTransactionDO.setResourceGroupId(branchSession.getResourceGroupId());
        branchTransactionDO.setTransactionId(branchSession.getTransactionId());
        branchTransactionDO.setApplicationData(branchSession.getApplicationData());
        branchTransactionDO.setResourceId(branchSession.getResourceId());
        branchTransactionDO.setStatus(branchSession.getStatus().getCode());
        return branchTransactionDO;
    }
}
