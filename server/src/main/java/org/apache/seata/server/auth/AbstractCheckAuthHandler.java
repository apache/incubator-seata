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
package org.apache.seata.server.auth;

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.rpc.RegisterCheckAuthHandler;

import static org.apache.seata.common.DefaultValues.DEFAULT_SERVER_ENABLE_CHECK_AUTH;

/**
 */
public abstract class AbstractCheckAuthHandler implements RegisterCheckAuthHandler {

    private static final Boolean ENABLE_CHECK_AUTH = ConfigurationFactory.getInstance().getBoolean(
        ConfigurationKeys.SERVER_ENABLE_CHECK_AUTH, DEFAULT_SERVER_ENABLE_CHECK_AUTH);

    @Override
    public boolean regTransactionManagerCheckAuth(RegisterTMRequest request) {
        if (!ENABLE_CHECK_AUTH) {
            return true;
        }
        return doRegTransactionManagerCheck(request);
    }

    public abstract boolean doRegTransactionManagerCheck(RegisterTMRequest request);

    @Override
    public boolean regResourceManagerCheckAuth(RegisterRMRequest request) {
        if (!ENABLE_CHECK_AUTH) {
            return true;
        }
        return doRegResourceManagerCheck(request);
    }

    public abstract boolean doRegResourceManagerCheck(RegisterRMRequest request);
}
