package io.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_HBASE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_HBASE_POOL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_HBASE_TABLE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_HBASE_STATUS_TABLE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_HBASE_LOCK_KEY_TABLE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_HBASE_LOCK_TABLE_PREFIX;

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
    private String namespace;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

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
    @Component
    @ConfigurationProperties(prefix = STORE_HBASE_TABLE_PREFIX)
    public static class Table {
        private String name;
        private String globalCF;
        private String branchesCF;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGlobalCF() {
            return globalCF;
        }

        public void setGlobalCF(String globalCF) {
            this.globalCF = globalCF;
        }

        public String getBranchesCF() {
            return branchesCF;
        }

        public void setBranchesCF(String branchesCF) {
            this.branchesCF = branchesCF;
        }

    }

    @Component
    @ConfigurationProperties(prefix = STORE_HBASE_STATUS_TABLE_PREFIX)
    public static class Status {
        private String name;
        private String transactionIdCF;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTransactionIdCF() {
            return transactionIdCF;
        }

        public void setTransactionIdCF(String transactionIdCF) {
            this.transactionIdCF = transactionIdCF;
        }
    }

    @Component
    @ConfigurationProperties(prefix = STORE_HBASE_LOCK_TABLE_PREFIX)
    public static class Lock {
        private String name;
        private String lockCF;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLockCF() {
            return lockCF;
        }

        public void setLockCF(String lockCF) {
            this.lockCF = lockCF;
        }
    }

    @Component
    @ConfigurationProperties(prefix = STORE_HBASE_LOCK_KEY_TABLE_PREFIX)
    public static class LockKey {
        private String name;
        private String transactionIdCF;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTransactionIdCF() {
            return transactionIdCF;
        }

        public void setTransactionIdCF(String transactionIdCF) {
            this.transactionIdCF = transactionIdCF;
        }
    }

}
