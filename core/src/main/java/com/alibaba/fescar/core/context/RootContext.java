/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.context;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootContext.class);

    public static final String KEY_XID = "TX_XID";

    private static ContextCore CONTEXT_HOLDER = ContextCoreLoader.load();

    public static String getXID() {
        return CONTEXT_HOLDER.get(KEY_XID);
    }

    public static void bind(String xid) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind " + xid);
        }
        CONTEXT_HOLDER.put(KEY_XID, xid);
    }

    public static String unbind() {
        String xid = CONTEXT_HOLDER.remove(KEY_XID);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("unbind " + xid);
        }
        return xid;
    }

    public static boolean inGlobalTransaction() {
        return CONTEXT_HOLDER.get(KEY_XID) != null;
    }

    public static void assertNotInGlobalTransaction() {
        if (inGlobalTransaction()) {
            throw new ShouldNeverHappenException();
        }
    }
}
