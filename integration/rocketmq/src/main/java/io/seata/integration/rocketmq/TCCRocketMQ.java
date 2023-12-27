package io.seata.integration.rocketmq;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import java.net.UnknownHostException;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * The interface Tcc rocket mq.
 *
 */
@LocalTCC
public interface TCCRocketMQ {

    void setDefaultMQProducer(DefaultMQProducer producer);

    /**
     * RocketMQ half send
     *
     * @param context thre context
     * @param message the message
     * @return SendResult
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws InterruptedException
     * @throws MQClientException
     */
    @TwoPhaseBusinessAction(name = "tccRocketMQ", commitMethod = "commit", rollbackMethod = "rollback")
    SendResult prepare(BusinessActionContext context, Message message)
            throws MQBrokerException, RemotingException, InterruptedException, MQClientException;

    /**
     * RocketMQ half send commit
     *
     * @param context the BusinessActionContext
     * @return SendResult
     * @throws UnknownHostException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws InterruptedException
     */
    boolean commit(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException;

    /**
     * RocketMQ half send rollback
     *
     * @param context the BusinessActionContext
     * @return
     * @throws UnknownHostException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws InterruptedException
     */
    boolean rollback(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException;
}