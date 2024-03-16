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
package io.seata.spring.annotation;

import io.seata.rm.RMClient;
import io.seata.tm.TMClient;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.tm.api.FailureHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP_OLD;

public class GlobalTransactionScanner extends org.apache.seata.spring.annotation.GlobalTransactionScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionScanner.class);

    public GlobalTransactionScanner(String txServiceGroup) {
        super(txServiceGroup);
    }

    public GlobalTransactionScanner(String txServiceGroup, int mode) {
        super(txServiceGroup, mode);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup) {
        super(applicationId, txServiceGroup);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode) {
        super(applicationId, txServiceGroup, mode);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup, FailureHandler failureHandlerHook) {
        super(applicationId, txServiceGroup, failureHandlerHook);
    }

    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode, FailureHandler failureHandlerHook) {
        super(applicationId, txServiceGroup, mode, failureHandlerHook);
    }

    protected void initClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (DEFAULT_TX_GROUP_OLD.equals(getTxServiceGroup())) {
            LOGGER.warn("the default value of seata.tx-service-group: {} has already changed to {} since Seata 1.5, " +
                            "please change your default configuration as soon as possible " +
                            "and we don't recommend you to use default tx-service-group's value provided by seata",
                    DEFAULT_TX_GROUP_OLD, DEFAULT_TX_GROUP);
        }
        if (StringUtils.isNullOrEmpty(getApplicationId()) || StringUtils.isNullOrEmpty(getTxServiceGroup())) {
            throw new IllegalArgumentException(String.format("applicationId: %s, txServiceGroup: %s", getApplicationId(), getTxServiceGroup()));
        }
        //init TM
        TMClient.init(getApplicationId(), getTxServiceGroup(), getAccessKey(), getSecretKey());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Transaction Manager Client is initialized. applicationId[{}] txServiceGroup[{}]", getApplicationId(), getTxServiceGroup());
        }
        //init RM
        RMClient.init(getApplicationId(), getTxServiceGroup());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Resource Manager is initialized. applicationId[{}] txServiceGroup[{}]", getApplicationId(), getTxServiceGroup());
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Global Transaction Clients are initialized. ");
        }
        registerSpringShutdownHook();
    }
}
