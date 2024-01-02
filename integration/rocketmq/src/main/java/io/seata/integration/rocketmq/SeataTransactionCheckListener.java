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
package io.seata.integration.rocketmq;

import io.seata.common.ConfigurationKeys;
import io.seata.common.DefaultValues;
import io.seata.config.ConfigurationFactory;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionCheckListener;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seata TransactionCheckListener
 **/
public class SeataTransactionCheckListener implements TransactionCheckListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataTransactionListener.class);

    private static final int ROCKET_MQ_MSG_TIMEOUT = ConfigurationFactory.getInstance().getInt(ConfigurationKeys.ROCKET_MQ_MSG_TIMEOUT,
            DefaultValues.DEFAULT_ROCKET_MQ_MSG_TIMEOUT);

    private TransactionCheckListener listener;

    public SeataTransactionCheckListener(TransactionCheckListener listener) {
        this.listener = listener;
    }

    @Override
    public LocalTransactionState checkLocalTransactionState(MessageExt msg) {
        String inGlobalTransaction = msg.getProperty(SeataRocketMQConst.PROP_KEY_IN_GLOBAL_TRANSACTION);
        String sendTime = msg.getProperty(SeataRocketMQConst.PROP_KEY_MSG_SEND_TIME);
        // msg是seata相关的
        if ("true".equals(inGlobalTransaction)) {
            if(System.currentTimeMillis() - Long.parseLong(sendTime) > ROCKET_MQ_MSG_TIMEOUT){
                LOGGER.error("rocketmq sent a message in global transaction, and it is timeout, return ROLLBACK_MESSAGE");
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }else {
                LOGGER.info("checkLocalTransaction state=COMMIT_MESSAGE, but global transaction not complete,return UNKNOW");
                return LocalTransactionState.UNKNOW;
            }
        }
        return listener.checkLocalTransactionState(msg);
    }
}
