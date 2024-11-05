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
package io.seata.core.context;


import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.core.model.BranchType;

/**
 * The type Root context.
 */
@Deprecated
public class RootContext {

    /**
     * The constant KEY_XID.
     * used for apache dubbo
     */
    public static final String KEY_XID = "TX_XID";

    /**
     * The constant KEY_BRANCH_TYPE
     * * used for apache dubbo
     */
    public static final String KEY_BRANCH_TYPE = "TX_BRANCH_TYPE";

    private static BranchType convertIoSeata(org.apache.seata.core.model.BranchType branchType) {
        if (branchType == null) {
            return null;
        } else {
            return BranchType.get(branchType.name());
        }
    }

    /**
     * Sets default branch type.
     *
     * @param defaultBranchType the default branch type
     */
    public static void setDefaultBranchType(BranchType defaultBranchType) {
        org.apache.seata.core.context.RootContext.setDefaultBranchType(defaultBranchType.convertBranchType());
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    @Nullable
    public static String getXID() {
        return org.apache.seata.core.context.RootContext.getXID();
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    public static void bind(@Nonnull String xid) {
        org.apache.seata.core.context.RootContext.bind(xid);
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public static Integer getTimeout() {
        return org.apache.seata.core.context.RootContext.getTimeout();
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    public static void setTimeout(Integer timeout) {
        org.apache.seata.core.context.RootContext.setTimeout(timeout);
    }

    /**
     * Bind global lock flag.
     */
    public static void bindGlobalLockFlag() {
        org.apache.seata.core.context.RootContext.bindGlobalLockFlag();
    }

    /**
     * Unbind string.
     *
     * @return the string
     */
    @Nullable
    public static String unbind() {
        return org.apache.seata.core.context.RootContext.unbind();
    }

    /**
     * Unbind global lock flag.
     */
    public static void unbindGlobalLockFlag() {
        org.apache.seata.core.context.RootContext.unbindGlobalLockFlag();
    }

    /**
     * In global transaction boolean.
     *
     * @return the boolean
     */
    public static boolean inGlobalTransaction() {
        return org.apache.seata.core.context.RootContext.inGlobalTransaction();
    }

    /**
     * In tcc branch boolean.
     *
     * @return the boolean
     */
    public static boolean inTccBranch() {
        return org.apache.seata.core.context.RootContext.inTccBranch();
    }

    /**
     * In saga branch boolean.
     *
     * @return the boolean
     */
    public static boolean inSagaBranch() {
        return org.apache.seata.core.context.RootContext.inSagaBranch();
    }

    /**
     * Gets branch type.
     *
     * @return the branch type
     */
    @Nullable
    public static BranchType getBranchType() {
        return convertIoSeata(org.apache.seata.core.context.RootContext.getBranchType());
    }

    /**
     * Bind branch type.
     *
     * @param branchType the branch type
     */
    public static void bindBranchType(@Nonnull BranchType branchType) {
        org.apache.seata.core.context.RootContext.bindBranchType(branchType.convertBranchType());
    }

    /**
     * Unbind branch type branch type.
     *
     * @return the branch type
     */
    @Nullable
    public static BranchType unbindBranchType() {
        return convertIoSeata(org.apache.seata.core.context.RootContext.unbindBranchType());
    }

    /**
     * Require global lock boolean.
     *
     * @return the boolean
     */
    public static boolean requireGlobalLock() {
        return org.apache.seata.core.context.RootContext.requireGlobalLock();
    }

    /**
     * Assert not in global transaction.
     */
    public static void assertNotInGlobalTransaction() {
        org.apache.seata.core.context.RootContext.assertNotInGlobalTransaction();
    }

    /**
     * Entries map.
     *
     * @return the map
     */
    public static Map<String, Object> entries() {
        return org.apache.seata.core.context.RootContext.entries();
    }
}
