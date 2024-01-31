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

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.protocol.AbstractIdentifyRequest;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@LoadLevel(name = "defaultCheckAuthHandler", order = 1)
public class DefaultCheckAuthHandler extends AbstractCheckAuthHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCheckAuthHandler.class);

    @Override
    public boolean doRegTransactionManagerCheck(RegisterTMRequest request) {
        return true;
    }

    @Override
    public boolean doRegResourceManagerCheck(RegisterRMRequest request) {
        return true;
    }

    @Override
    public boolean needRefreshToken(AbstractIdentifyRequest abstractIdentifyRequest) {
        return false;
    }

    @Override
    public String refreshToken(AbstractIdentifyRequest abstractIdentifyRequest) {
        LOGGER.error("This method is not supported.");
        return null;
    }
}
