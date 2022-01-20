package io.seata.spring.boot.autoconfigure.properties.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_REDIS_PREFIX;


/**
 * <p>
 *     redis配置中心
 * </p>
 * @author wangyuewen
 */
@Component
@ConfigurationProperties(prefix = CONFIG_REDIS_PREFIX)
public class ConfigRedisProperties {
    private String serverAddr = "localhost:6379";
    private int db = 0;
    private String password = "";
    private int timeout = 0;
    private String keyName = "seataConfig";
    private Boolean listenerEnabled = Boolean.FALSE;

    public ConfigRedisProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public ConfigRedisProperties setDb(int db) {
        this.db = db;
        return this;
    }

    public ConfigRedisProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public ConfigRedisProperties setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ConfigRedisProperties setKeyName(String keyName) {
        this.keyName = keyName;
        return this;
    }

    public ConfigRedisProperties setListenerEnabled(Boolean listenerEnabled){
        this.listenerEnabled = listenerEnabled;
        return this;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public int getDb() {
        return db;
    }

    public String getPassword() {
        return password;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getKeyName() {
        return keyName;
    }

    public Boolean getListenerEnabled(){
        return listenerEnabled;
    }
}
