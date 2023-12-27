package io.seata.integration.rocketmq;

import io.seata.core.context.RootContext;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * seata send message hook
 *
 */
public class SeataSendMessageHook implements SendMessageHook {
    private TCCRocketMQ tccRocketMQ;

    public SeataSendMessageHook(TCCRocketMQ tccRocketMQ){
        this.tccRocketMQ = tccRocketMQ;
    }

    @Override
    public String hookName() {
        return "SeataSendMessageHook";
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        if (RootContext.inGlobalTransaction()) {
            try {
                tccRocketMQ.prepare(null, context.getMessage());
            } catch (MQBrokerException e) {
                throw new RuntimeException(e);
            } catch (RemotingException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (MQClientException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void sendMessageAfter(SendMessageContext sendMessageContext) {

    }

}
