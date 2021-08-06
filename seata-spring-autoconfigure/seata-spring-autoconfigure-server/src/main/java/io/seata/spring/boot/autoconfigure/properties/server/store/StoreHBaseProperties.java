package io.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.*;

/**
 * ClassName: StoreHBaseProperties
 * Description:
 *
 * @author haishin
 */
@Component
@ConfigurationProperties(prefix = STORE_HBASE_PREFIX)
public class StoreHBaseProperties {
    private String zookeeperQuorum;
    private Integer propertyClientPort;

    public String getZookeeperQuorum() {
        return zookeeperQuorum;
    }

    public void setZookeeperQuorum(String zookeeperQuorum) {
        this.zookeeperQuorum = zookeeperQuorum;
    }

    public Integer getPropertyClientPort() {
        return propertyClientPort;
    }

    public void setPropertyClientPort(Integer propertyClientPort) {
        this.propertyClientPort = propertyClientPort;
    }

    @Component
    @ConfigurationProperties(prefix = STORE_HBASE_POOL_PREFIX)
    public static class Pool {
        private String type;
        private Integer size;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }
    }
}
