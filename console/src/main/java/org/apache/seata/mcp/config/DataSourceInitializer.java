package org.apache.seata.mcp.config;

import org.apache.seata.mcp.store.DataSourceFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DataSourceInitializer {

    @PostConstruct
    public void init() {
        DataSourceFactory.initAllDataSources();
    }
}
