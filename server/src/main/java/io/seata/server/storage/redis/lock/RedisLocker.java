package io.seata.server.storage.redis.lock;

import java.util.ArrayList;
import java.util.List;
import com.alibaba.fastjson.JSON;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.server.session.BranchSession;
import io.seata.server.storage.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;

public class RedisLocker extends AbstractLocker {

    private static Integer DEFAULT_SECONDS = 30000;

    private static String DEFAULT_REDIS_SEATA_LOCK_PREFIX = "SEATA_LOCK_";
    /**
     * The Branch session.
     */
    protected BranchSession branchSession = null;

    /**
     * Instantiates a new Memory locker.
     *
     * @param branchSession the branch session
     */
    public RedisLocker(BranchSession branchSession) {
        this.branchSession = branchSession;
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            //no lock
            return true;
        }
        List<String> successList = new ArrayList<>();
        long status = 0;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            for (RowLock lock : rowLocks) {
                status = jedis.setnx(DEFAULT_REDIS_SEATA_LOCK_PREFIX+lock.getRowKey(), JSON.toJSONString(lock));
                if (status == 1) {
                    successList.add(DEFAULT_REDIS_SEATA_LOCK_PREFIX+lock.getRowKey());
                    jedis.expire(DEFAULT_REDIS_SEATA_LOCK_PREFIX+lock.getRowKey(), DEFAULT_SECONDS);
                } else {
                    break;
                }
            }
            if (status != 1) {
                jedis.del(successList.toArray(new String[successList.size()]));
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            //no lock
            return true;
        }
        String[] keys=new String[rowLocks.size()];
        for (int i=0;i<rowLocks.size();i++){
            keys[i]=DEFAULT_REDIS_SEATA_LOCK_PREFIX+rowLocks.get(i).getRowKey();
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.del(keys);
            return true;
        }
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            //no lock
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            for (RowLock rowlock : rowLocks) {
                String rowlockJson = jedis.get(DEFAULT_REDIS_SEATA_LOCK_PREFIX + rowlock.getRowKey());
                if (StringUtils.isNotBlank(rowlockJson)) {
                    RowLock lock = JSON.parseObject(rowlockJson, RowLock.class);
                    if (!lock.getXid().equals(rowlock.getXid())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
