package io.seata.integration.rocketmq;

/**
 * ?
 *
 **/
public class TCCRocketMQHolder {
    private static TCCRocketMQ tccRocketMQ;

    public static void setTCCRocketMQ(TCCRocketMQ tccRocketMQ) {
        TCCRocketMQHolder.tccRocketMQ = tccRocketMQ;
    }

    public static TCCRocketMQ getTCCRocketMQ() {
        return tccRocketMQ;
    }
}
