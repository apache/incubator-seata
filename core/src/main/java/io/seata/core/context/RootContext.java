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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.Constants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static io.seata.core.model.BranchType.AT;
import static io.seata.core.model.BranchType.XA;

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
     * The constant HIDDEN_KEY_XID for sofa-rpc integration.
     */
    public static final String HIDDEN_KEY_XID = Constants.HIDE_KEY_PREFIX_CHAR + KEY_XID;

    /**
     * The constant KEY_TIMEOUT.
     */
    public static final String KEY_TIMEOUT = "TX_TIMEOUT";

    /**
     * The constant MDC_KEY_XID for logback
     * @since 1.5.0
     */
    public static final String MDC_KEY_XID = "X-TX-XID";

    /**
     * The constant MDC_KEY_BRANCH_ID for logback
     * @since 1.5.0
     */
    public static final String MDC_KEY_BRANCH_ID = "X-TX-BRANCH-ID";

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

    private static BranchType DEFAULT_BRANCH_TYPE;

    public static void setDefaultBranchType(BranchType defaultBranchType) {
        if (defaultBranchType != AT && defaultBranchType != XA) {
            throw new IllegalArgumentException("The default branch type must be " + AT + " or " + XA + "." +
                " the value of the argument is: " + defaultBranchType);
        }
        if (DEFAULT_BRANCH_TYPE != null && DEFAULT_BRANCH_TYPE != defaultBranchType && LOGGER.isWarnEnabled()) {
            LOGGER.warn("The `{}.DEFAULT_BRANCH_TYPE` has been set repeatedly. The value changes from {} to {}",
                RootContext.class.getSimpleName(), DEFAULT_BRANCH_TYPE, defaultBranchType);
        }
        DEFAULT_BRANCH_TYPE = defaultBranchType;
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    @Nullable
    public static String getXID() {
        return (String) CONTEXT_HOLDER.get(KEY_XID);
    }

    /**
     * Bind xid.
     *
     * @param xid the xid
     */
    public static void bind(@Nonnull String xid) {
        if (StringUtils.isBlank(xid)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("xid is blank, switch to unbind operation!");
            }
            unbind();
        } else {
            MDC.put(MDC_KEY_XID, xid);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bind {}", xid);
            }
            CONTEXT_HOLDER.put(KEY_XID, xid);
        }
    }

    public static Integer getTimeout() {
        return (Integer) CONTEXT_HOLDER.get(KEY_TIMEOUT);
    }

    public static void setTimeout(Integer timeout) {
        CONTEXT_HOLDER.put(KEY_TIMEOUT,timeout);
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
     * Unbind xid.
     *
     * @return the previous xid or null
     */
    @Nullable
    public static String unbind() {
        String xid = (String) CONTEXT_HOLDER.remove(KEY_XID);
        if (xid != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("unbind {} ", xid);
            }
            MDC.remove(MDC_KEY_XID);
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
    @Nullable
    public static BranchType getBranchType() {
        if (inGlobalTransaction()) {
            BranchType branchType = (BranchType) CONTEXT_HOLDER.get(KEY_BRANCH_TYPE);
            if (branchType != null) {
                return branchType;
            }
            //Returns the default branch type.
            return DEFAULT_BRANCH_TYPE != null ? DEFAULT_BRANCH_TYPE : BranchType.AT;
        }
        return null;
    }

    /**
     * bind branch type
     *
     * @param branchType the branch type
     */
    public static void bindBranchType(@Nonnull BranchType branchType) {
        if (branchType == null) {
            throw new IllegalArgumentException("branchType must be not null");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind branch type {}", branchType);
        }

        CONTEXT_HOLDER.put(KEY_BRANCH_TYPE, branchType);
    }

    /**
     * unbind branch type
     *
     * @return the previous branch type or null
     */
    @Nullable
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
     * @return the boolean
     */
    public static boolean requireGlobalLock() {
        return CONTEXT_HOLDER.get(KEY_GLOBAL_LOCK_FLAG) != null;
    }

    /**
     * Assert not in global transaction.
     */
    public static void assertNotInGlobalTransaction() {
        if (inGlobalTransaction()) {
            throw new ShouldNeverHappenException(String.format("expect has not xid, but was:%s",
                CONTEXT_HOLDER.get(KEY_XID)));
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
