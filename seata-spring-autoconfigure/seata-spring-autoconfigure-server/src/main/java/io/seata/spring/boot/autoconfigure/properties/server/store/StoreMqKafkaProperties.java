package io.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.ConfigurationKeys.STORE_MQ_KAFKA_PREFIX;

@Component
@ConfigurationProperties(prefix = STORE_MQ_KAFKA_PREFIX)
public class StoreMqKafkaProperties {
    private String servers = "localhost:9092";

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }
}
