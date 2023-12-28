package io.seata.integration.rocketmq;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seata Transaction Listener
 **/
public class SeataTransactionListener implements TransactionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataTransactionListener.class);

    private TransactionListener listener;


    public SeataTransactionListener(TransactionListener listener) {
        this.listener = listener;
    }

    @Override
    public final LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        return listener.executeLocalTransaction(msg, arg);
    }


    @Override
    public final LocalTransactionState checkLocalTransaction(MessageExt msg) {
        LocalTransactionState state = listener.checkLocalTransaction(msg);
        String inGlobalTransaction = msg.getProperty(SeataRocketMQConst.PROP_KEY_IN_GLOBAL_TRANSACTION);
        if ("true".equals(inGlobalTransaction) && state == LocalTransactionState.COMMIT_MESSAGE) {
            LOGGER.info("checkLocalTransaction state=COMMIT_MESSAGE, but global transaction not complete,return UNKNOW");
            return LocalTransactionState.UNKNOW;
        }
        return state;
    }

}
