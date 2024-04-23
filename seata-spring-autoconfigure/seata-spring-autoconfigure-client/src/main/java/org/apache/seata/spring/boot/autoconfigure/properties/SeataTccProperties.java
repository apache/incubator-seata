package org.apache.seata.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.TCC_PREFIX;

@Component
@ConfigurationProperties(prefix = TCC_PREFIX)
public class SeataTccProperties {
    private String contextJsonParserType;

    public String getContextJsonParserType() {
        return contextJsonParserType;
    }

    public void setContextJsonParserType(String contextJsonParserType) {
        this.contextJsonParserType = contextJsonParserType;
    }
}
