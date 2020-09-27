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
package io.seata.core.context;

import java.util.Map;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.common.DefaultValues;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * The type Root context.
 *
 * @author slievrly
 */
public class RootContext {

    private RootContext() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RootContext.class);

    /**
     * The constant KEY_XID.
     */
    public static final String KEY_XID = "TX_XID";

    /**
     * The constant KEY_BRANCH_TYPE
     */
    public static final String KEY_BRANCH_TYPE = "TX_BRANCH_TYPE";

    /**
     * The constant KEY_GLOBAL_LOCK_FLAG, VALUE_GLOBAL_LOCK_FLAG
     */
    public static final String KEY_GLOBAL_LOCK_FLAG = "TX_LOCK";
    public static final Boolean VALUE_GLOBAL_LOCK_FLAG = true;

    private static ContextCore CONTEXT_HOLDER = ContextCoreLoader.load();

    private static final String DATA_SOURCE_PROXY_MODE = ConfigurationFactory.getInstance()
            .getConfig(ConfigurationKeys.DATA_SOURCE_PROXY_MODE, DefaultValues.DEFAULT_DATA_SOURCE_PROXY_MODE);

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public static String getXID() {
        String xid = (String) CONTEXT_HOLDER.get(KEY_XID);
        if (StringUtils.isNotBlank(xid)) {
            return xid;
        }
        return null;
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    public static void bind(@Nonnull String xid) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind {}", xid);
        }
        CONTEXT_HOLDER.put(KEY_XID, xid);
    }

    /**
     * declare local transactions will use global lock check for update/delete/insert/selectForUpdate SQL
     */
    public static void bindGlobalLockFlag() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Local Transaction Global Lock support enabled");
        }

        //just put something not null
        CONTEXT_HOLDER.put(KEY_GLOBAL_LOCK_FLAG, VALUE_GLOBAL_LOCK_FLAG);
    }

    /**
     * Unbind string.
     *
     * @return the string
     */
    public static String unbind() {
        String xid = (String) CONTEXT_HOLDER.remove(KEY_XID);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("unbind {} ", xid);
        }
        return xid;
    }

    public static void unbindGlobalLockFlag() {
        Boolean lockFlag = (Boolean) CONTEXT_HOLDER.remove(KEY_GLOBAL_LOCK_FLAG);
        if (LOGGER.isDebugEnabled() && lockFlag != null) {
            LOGGER.debug("unbind global lock flag");
        }
    }

    /**
     * In global transaction boolean.
     *
     * @return the boolean
     */
    public static boolean inGlobalTransaction() {
        return CONTEXT_HOLDER.get(KEY_XID) != null;
    }

    /**
     * In tcc branch boolean.
     *
     * @return the boolean
     */
    public static boolean inTccBranch() {
        return BranchType.TCC == getBranchType();
    }

    /**
     * In saga branch boolean.
     *
     * @return the boolean
     */
    public static boolean inSagaBranch() {
        return BranchType.SAGA == getBranchType();
    }

    /**
     * get the branch type
     *
     * @return the branch type String
     */
    public static BranchType getBranchType() {
        if (inGlobalTransaction()) {
            BranchType branchType = (BranchType) CONTEXT_HOLDER.get(KEY_BRANCH_TYPE);
            if (branchType != null) {
                return branchType;
            }
            //default branchType is the dataSourceProxyMode
            return BranchType.XA.name().equalsIgnoreCase(DATA_SOURCE_PROXY_MODE) ? BranchType.XA : BranchType.AT;
        }
        return null;
    }

    /**
     * bind branch type
     *
     * @param branchType the branch type
     */
    public static void bindBranchType(@Nonnull BranchType branchType) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind branch type {}", branchType);
        }

        CONTEXT_HOLDER.put(KEY_BRANCH_TYPE, branchType);
    }

    /**
     * unbind branch type
     *
     * @return the previous branch type string
     */
    public static BranchType unbindBranchType() {
        BranchType unbindBranchType = (BranchType) CONTEXT_HOLDER.remove(KEY_BRANCH_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("unbind branch type {}", unbindBranchType);
        }
        return unbindBranchType;
    }

    /**
     * requires global lock check
     *
     * @return
     */
    public static boolean requireGlobalLock() {
        return CONTEXT_HOLDER.get(KEY_GLOBAL_LOCK_FLAG) != null;
    }

    /**
     * Assert not in global transaction.
     */
    public static void assertNotInGlobalTransaction() {
        if (inGlobalTransaction()) {
            throw new ShouldNeverHappenException();
        }
    }

    /**
     * entry map
     *
     * @return the key-value map
     */
    public static Map<String, Object> entries() {
        return CONTEXT_HOLDER.entries();
    }
}
