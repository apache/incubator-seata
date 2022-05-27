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

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.DefaultResourceManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author funkye
 */
public class JedisPoolProxy extends JedisPool implements Resource {

    private JedisPool targetJedisPool;

    private String resourceId;

    private String resourceGroupId;

    public JedisPoolProxy(JedisPool jedisPool, String resourceId) {
        this.resourceId = resourceId;
        this.targetJedisPool = jedisPool;
        DefaultResourceManager.get().registerResource(this);
    }

    @Override
    public Jedis getResource() {
        Jedis jedis = super.getResource();
        return StringUtils.isNotBlank(RootContext.getXID()) ? new JedisProxy(jedis, this) : jedis;
    }

    @Override
    protected void returnBrokenResource(final Jedis resource) {
        Jedis realResource = resource instanceof JedisProxy ? ((JedisProxy)resource).getJedis() : resource;
        if (realResource != null) {
            returnBrokenResourceObject(realResource);
        }
    }

    @Override
    protected void returnResource(final Jedis resource) {
        Jedis realResource = resource instanceof JedisProxy ? ((JedisProxy)resource).getJedis() : resource;
        if (realResource != null) {
            try {
                realResource.resetState();
                returnResourceObject(realResource);
            } catch (Exception e) {
                returnBrokenResource(realResource);
                throw new JedisException("Resource is returned to the pool as broken", e);
            }
        }
    }

    public JedisPool getTargetJedisPool() {
        return targetJedisPool;
    }

    public void setTargetJedisPool(JedisPool targetJedisPool) {
        this.targetJedisPool = targetJedisPool;
    }

    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.ATbyJedis;
    }
}
