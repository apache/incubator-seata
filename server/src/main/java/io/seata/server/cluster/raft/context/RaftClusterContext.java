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
package io.seata.server.cluster.raft.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.core.context.ContextCore;
import io.seata.core.context.ContextCoreLoader;

/**
 * @author funkye
 */
public class RaftClusterContext {

    private RaftClusterContext() {
    }

    /**
     * The constant KEY_GROUP.
     */
    public static final String KEY_GROUP = "TX_GROUP";

    private static ContextCore CONTEXT_HOLDER = ContextCoreLoader.load();

    /**
     * Bind group.
     *
     * @param group the group
     */
    public static void bindGroup(@Nonnull String group) {
        CONTEXT_HOLDER.put(KEY_GROUP, group);
    }

    /**
     * Unbind group.
     */
    public static void unbindGroup() {
        CONTEXT_HOLDER.remove(KEY_GROUP);
    }

    @Nullable
    public static String getGroup() {
        return (String) CONTEXT_HOLDER.get(KEY_GROUP);
    }


}