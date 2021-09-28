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
package io.seata.rm.tcc.rocketmq;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import java.net.UnknownHostException;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

@LocalTCC
public interface TCCRocketMQ {
    /**
     * RocketMQ half send
     *
     * @param context thre context
     * @param message the message
     * @return SendResult
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws NoSuchFieldException
     * @throws InterruptedException
     * @throws MQClientException
     */
    @TwoPhaseBusinessAction(name = "tccRocketMQ", commitMethod = "commit", rollbackMethod = "rollback")
    SendResult prepare(BusinessActionContext context, Message message)
        throws MQBrokerException, RemotingException, NoSuchFieldException, InterruptedException, MQClientException;

    /**
     * RocketMQ half send commit
     *
     * @param context the BusinessActionContext
     * @return SendResult
     * @throws UnknownHostException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws NoSuchFieldException
     * @throws InterruptedException
     */
    boolean commit(BusinessActionContext context)
        throws UnknownHostException, MQBrokerException, RemotingException, NoSuchFieldException, InterruptedException;

    /**
     * RocketMQ half send rollback
     *
     * @param context the BusinessActionContext
     * @return
     * @throws UnknownHostException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws NoSuchFieldException
     * @throws InterruptedException
     */
    boolean rollback(BusinessActionContext context)
        throws UnknownHostException, MQBrokerException, RemotingException, NoSuchFieldException, InterruptedException;
}
