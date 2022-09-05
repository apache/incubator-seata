package io.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_INFLUXDB_PREFIX;

/**
 * @author qijun@apache.org
 */
@Component
@ConfigurationProperties(prefix = STORE_INFLUXDB_PREFIX)
public class StoreInfluxDBProperties {
    private String username = "admin";
    private String password = "admin";
    private String openurl = "127.0.0.1";
    private String database = "seata";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOpenurl() {
        return openurl;
    }

    public void setOpenurl(String openurl) {
        this.openurl = openurl;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}