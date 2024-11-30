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
package io.seata.core.rpc.netty;

import java.util.HashMap;
import java.util.Map;

import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.stereotype.Service;

/**
 * The type Action1.
 */
@Service
public class Action1Impl implements Action1 {

    private static final Map<String, Integer> COMMIT_MAP = new HashMap<>();
    private static final Map<String, Integer> ROLLBACK_MAP = new HashMap<>();

    @Override
    public String insert(Long reqId, Map<String, String> params) {
        System.out.println("prepare");
        return "prepare";
    }

    @Override
    public boolean commitTcc(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        System.out.println("commitTcc:" + xid + "," + actionContext.getActionContext());
        COMMIT_MAP.compute(xid, (k, v) -> v == null ? 1 : v + 1);
        return true;
    }

    @Override
    public boolean cancel(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        System.out.println("cancelTcc:" + xid + "," + actionContext.getActionContext());
        ROLLBACK_MAP.compute(xid, (k, v) -> v == null ? 1 : v + 1);
        return true;
    }

    public static int getCommitTimes(String xid) {
        return COMMIT_MAP.getOrDefault(xid, 0);
    }

    public static int getRollbackTimes(String xid) {
        return ROLLBACK_MAP.getOrDefault(xid, 0);
    }
}
