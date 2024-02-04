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

import org.apache.seata.common.exception.NotSupportYetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SeataMQProducer Factory
 **/
public class SeataMQProducerFactory {

    private static TCCRocketMQ tccRocketMQ;

    private static Map<String, SeataMQProducer> PRODUCER_MAP = new ConcurrentHashMap<>();
    private volatile static String SINGLE_PRODUCER_ID;

    public SeataMQProducer create(String groupName, String producerId) {
        if (SINGLE_PRODUCER_ID == null) {
            synchronized (SeataMQProducerFactory.class) {
                if (SINGLE_PRODUCER_ID == null) {
                    SINGLE_PRODUCER_ID = producerId;
                    SeataMQProducer producer = new SeataMQProducer(groupName);
                    tccRocketMQ.setProducer(producer);
                    PRODUCER_MAP.put(producerId, producer);
                }
            }
        }

        if (!SINGLE_PRODUCER_ID.equals(producerId)) {
            throw new NotSupportYetException("only one producer is allowed");
        }
        return getProducer();
    }

    public SeataMQProducer getProducer() {
        return PRODUCER_MAP.get(SINGLE_PRODUCER_ID);
    }

    public static void setTccRocketMQ(TCCRocketMQ tccRocket) {
        tccRocketMQ = tccRocket;
    }

    public static TCCRocketMQ getTccRocketMQ() {
        return tccRocketMQ;
    }
}
