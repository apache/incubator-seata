
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rocketmq")
public class RocketMQProperties {

    public static final String RMQ_SYS_TRACE_TOPIC = "RMQ_SYS_TRACE_TOPIC";


    /**
     * The name server for rocketMQ, formats: `host:port;host:port`.
     */
    private String nameServer;

    /**
     * Enum type for accessChannel, values: LOCAL, CLOUD
     */
    private String accessChannel;

    private Producer producer;

    /**
     * for pull consumer only
     *
     */
    private PullConsumer pullConsumer = new PullConsumer();
    /**
     * Configure enable listener or not.
     * In some particular cases, if you don't want the the listener is enabled when container startup,
     * the configuration pattern is like this :
     * rocketmq.push-consumer.listeners.<group-name>.<topic-name>.enabled=<boolean value, true or false>
     * <p>
     * the listener is enabled by default.
     *
     */
    private PushConsumer consumer = new PushConsumer();

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getAccessChannel() {
        return accessChannel;
    }

    public void setAccessChannel(String accessChannel) {
        this.accessChannel = accessChannel;
    }

    public RocketMQProperties.Producer getProducer() {
        return producer;
    }

    public void setProducer(RocketMQProperties.Producer producer) {
        this.producer = producer;
    }

    public PullConsumer getPullConsumer() {
        return pullConsumer;
    }

    public void setPullConsumer(PullConsumer pullConsumer) {
        this.pullConsumer = pullConsumer;
    }

    public PushConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(PushConsumer consumer) {
        this.consumer = consumer;
    }

    public static class Producer {

        /**
         * Group name of producer.
         */
        private String group;

        /**
         * Namespace for this MQ Producer instance.
         */
        private String namespace;

        /**
         * Millis of send message timeout.
         */
        private int sendMessageTimeout = 3000;

        /**
         * Compress message body threshold, namely, message body larger than 4k will be compressed on default.
         */
        private int compressMessageBodyThreshold = 1024 * 4;

        /**
         * Maximum number of retry to perform internally before claiming sending failure in synchronous mode.
         * This may potentially cause message duplication which is up to application developers to resolve.
         */
        private int retryTimesWhenSendFailed = 2;

        /**
         * <p> Maximum number of retry to perform internally before claiming sending failure in asynchronous mode. </p>
         * This may potentially cause message duplication which is up to application developers to resolve.
         */
        private int retryTimesWhenSendAsyncFailed = 2;

        /**
         * Indicate whether to retry another broker on sending failure internally.
         */
        private boolean retryNextServer = false;

        /**
         * Maximum allowed message size in bytes.
         */
        private int maxMessageSize = 1024 * 1024 * 4;

        /**
         * The property of "access-key".
         */
        private String accessKey;

        /**
         * The property of "secret-key".
         */
        private String secretKey;

        /**
         * Switch flag instance for message trace.
         */
        private boolean enableMsgTrace = false;

        /**
         * The name value of message trace topic.If you don't config,you can use the default trace topic name.
         */
        private String customizedTraceTopic = RMQ_SYS_TRACE_TOPIC;

        /**
         * The property of "tlsEnable".
         */
        private boolean tlsEnable = false;

        /**
         * The property of "instanceName".
         */
        private String instanceName = "DEFAULT";

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public int getSendMessageTimeout() {
            return sendMessageTimeout;
        }

        public void setSendMessageTimeout(int sendMessageTimeout) {
            this.sendMessageTimeout = sendMessageTimeout;
        }

        public int getCompressMessageBodyThreshold() {
            return compressMessageBodyThreshold;
        }

        public void setCompressMessageBodyThreshold(int compressMessageBodyThreshold) {
            this.compressMessageBodyThreshold = compressMessageBodyThreshold;
        }

        public int getRetryTimesWhenSendFailed() {
            return retryTimesWhenSendFailed;
        }

        public void setRetryTimesWhenSendFailed(int retryTimesWhenSendFailed) {
            this.retryTimesWhenSendFailed = retryTimesWhenSendFailed;
        }

        public int getRetryTimesWhenSendAsyncFailed() {
            return retryTimesWhenSendAsyncFailed;
        }

        public void setRetryTimesWhenSendAsyncFailed(int retryTimesWhenSendAsyncFailed) {
            this.retryTimesWhenSendAsyncFailed = retryTimesWhenSendAsyncFailed;
        }

        public boolean isRetryNextServer() {
            return retryNextServer;
        }

        public void setRetryNextServer(boolean retryNextServer) {
            this.retryNextServer = retryNextServer;
        }

        public int getMaxMessageSize() {
            return maxMessageSize;
        }

        public void setMaxMessageSize(int maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isEnableMsgTrace() {
            return enableMsgTrace;
        }

        public void setEnableMsgTrace(boolean enableMsgTrace) {
            this.enableMsgTrace = enableMsgTrace;
        }

        public String getCustomizedTraceTopic() {
            return customizedTraceTopic;
        }

        public void setCustomizedTraceTopic(String customizedTraceTopic) {
            this.customizedTraceTopic = customizedTraceTopic;
        }

        public boolean isTlsEnable() {
            return tlsEnable;
        }

        public void setTlsEnable(boolean tlsEnable) {
            this.tlsEnable = tlsEnable;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }
    }

    public static class PullConsumer {
        /**
         * Group name of consumer.
         */
        private String group;

        /**
         * Namespace for this MQ Consumer instance.
         */
        private String namespace;

        /**
         * Topic name of consumer.
         */
        private String topic;

        /**
         * Control message mode, if you want all subscribers receive message all message, broadcasting is a good choice.
         */
        private String messageModel = "CLUSTERING";

        /**
         * Control how to selector message.
         */
        private String selectorType = "TAG";

        /**
         * Control which message can be select.
         */
        private String selectorExpression = "*";

        /**
         * The property of "access-key".
         */
        private String accessKey;

        /**
         * The property of "secret-key".
         */
        private String secretKey;

        /**
         * Maximum number of messages pulled each time.
         */
        private int pullBatchSize = 10;

        /**
         * Switch flag instance for message trace.
         */
        private boolean enableMsgTrace = false;

        /**
         * The name value of message trace topic.If you don't config,you can use the default trace topic name.
         */
        private String customizedTraceTopic = RMQ_SYS_TRACE_TOPIC;

        /**
         * The property of "tlsEnable".
         */
        private boolean tlsEnable = false;

        /**
         * The property of "instanceName".
         */
        private String instanceName = "DEFAULT";

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getMessageModel() {
            return messageModel;
        }

        public void setMessageModel(String messageModel) {
            this.messageModel = messageModel;
        }

        public String getSelectorType() {
            return selectorType;
        }

        public void setSelectorType(String selectorType) {
            this.selectorType = selectorType;
        }

        public String getSelectorExpression() {
            return selectorExpression;
        }

        public void setSelectorExpression(String selectorExpression) {
            this.selectorExpression = selectorExpression;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public int getPullBatchSize() {
            return pullBatchSize;
        }

        public void setPullBatchSize(int pullBatchSize) {
            this.pullBatchSize = pullBatchSize;
        }

        public boolean isEnableMsgTrace() {
            return enableMsgTrace;
        }

        public void setEnableMsgTrace(boolean enableMsgTrace) {
            this.enableMsgTrace = enableMsgTrace;
        }

        public String getCustomizedTraceTopic() {
            return customizedTraceTopic;
        }

        public void setCustomizedTraceTopic(String customizedTraceTopic) {
            this.customizedTraceTopic = customizedTraceTopic;
        }

        public boolean isTlsEnable() {
            return tlsEnable;
        }

        public void setTlsEnable(boolean tlsEnable) {
            this.tlsEnable = tlsEnable;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }
    }

    public static class PushConsumer extends PullConsumer {
        /**
         * listener configuration container
         * the pattern is like this:
         * group1.topic1 = false
         * group2.topic2 = true
         * group3.topic3 = false
         */
        private Map<String, Map<String, Boolean>> listeners = new HashMap<>();

        public Map<String, Map<String, Boolean>> getListeners() {
            return listeners;
        }

        public void setListeners(Map<String, Map<String, Boolean>> listeners) {
            this.listeners = listeners;
        }
    }

}
