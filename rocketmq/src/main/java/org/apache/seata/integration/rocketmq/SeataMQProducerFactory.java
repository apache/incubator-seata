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
package org.apache.seata.integration.rocketmq;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.util.ProxyUtil;

/**
 * SeataMQProducer Factory
 **/
public class SeataMQProducerFactory {

    public static final String ROCKET_TCC_NAME = "tccRocketMQ";
    public static final BranchType ROCKET_BRANCH_TYPE = BranchType.TCC;

    /**
     * Default Producer, it can be replaced to Map after multi-resource is supported
     */
    private static SeataMQProducer defaultProducer;

    public static SeataMQProducer createSingle(String nameServer, String producerGroup) throws MQClientException {
        return createSingle(nameServer, null, producerGroup, null);
    }

    public static SeataMQProducer createSingle(String nameServer, String namespace,
                                               String groupName, RPCHook rpcHook) throws MQClientException {
        if (defaultProducer == null) {
            synchronized (SeataMQProducerFactory.class) {
                if (defaultProducer == null) {
                    defaultProducer = new SeataMQProducer(namespace, groupName, rpcHook);
                    defaultProducer.setNamesrvAddr(nameServer);
                    TCCRocketMQ tccRocketMQProxy = ProxyUtil.createProxy(new TCCRocketMQImpl());
                    tccRocketMQProxy.setProducer(defaultProducer);
                    defaultProducer.setTccRocketMQ(tccRocketMQProxy);
                    defaultProducer.start();
                    return defaultProducer;
                }
            }
        }
        throw new NotSupportYetException("only one seata producer is permitted");
    }

    public static SeataMQProducer getProducer() {
        return defaultProducer;
    }
}
