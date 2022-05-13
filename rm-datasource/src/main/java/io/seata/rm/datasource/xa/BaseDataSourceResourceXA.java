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
package io.seata.rm.datasource.xa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.sql.XADataSource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.model.BranchStatus;
import io.seata.rm.BaseDataSourceResource;

/**
 * @author jianbin.chen
 */
public abstract class BaseDataSourceResourceXA<T extends Holdable> extends BaseDataSourceResource
    implements Holder<T>, XADataSource {

    private static final Cache<String, BranchStatus> BRANCH_STATUS_CACHE =
        CacheBuilder.newBuilder().maximumSize(1024).expireAfterAccess(10, TimeUnit.MINUTES).build();
    protected XADataSource xaDataSource;
    private Map<String, T> keeper = new ConcurrentHashMap<>();

    public static void setBranchStatus(String xaBranchXid, BranchStatus branchStatus) {
        BRANCH_STATUS_CACHE.put(xaBranchXid, branchStatus);
    }

    public static BranchStatus getBranchStatus(String xaBranchXid) {
        return BRANCH_STATUS_CACHE.getIfPresent(xaBranchXid);
    }

    public static void remove(String xaBranchXid) {
        if (StringUtils.isNotBlank(xaBranchXid)) {
            BRANCH_STATUS_CACHE.invalidate(xaBranchXid);
        }
    }

    @Override
    public T hold(String key, T value) {
        if (value.isHeld()) {
            T x = keeper.get(key);
            if (x != value) {
                throw new ShouldNeverHappenException("something wrong with keeper, keeping[" + x + "] but[" + value
                    + "] is also kept with the same key[" + key + "]");
            }
            return value;
        }
        T x = keeper.put(key, value);
        value.setHeld(true);
        return x;
    }

    @Override
    public T release(String key, T value) {
        T x = keeper.remove(key);
        if (x != value) {
            throw new ShouldNeverHappenException(
                "something wrong with keeper, released[" + x + "] but[" + value + "] is wanted with key[" + key + "]");
        }
        value.setHeld(false);
        return x;
    }

    @Override
    public T lookup(String key) {
        return keeper.get(key);
    }

    public Map<String, T> getKeeper() {
        return keeper;
    }

    public XADataSource getXaDataSource() {
        return xaDataSource;
    }

    public void setXaDataSource(XADataSource xaDataSource) {
        this.xaDataSource = xaDataSource;
    }

}
