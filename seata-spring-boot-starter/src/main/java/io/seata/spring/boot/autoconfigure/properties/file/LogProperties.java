package io.seata.spring.boot.autoconfigure.properties.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.LOG_PREFIX;

/**
 * @author jsbxyyx
 */
@Component
@ConfigurationProperties(prefix = LOG_PREFIX)
public class LogProperties {

    private int exceptionRate = 100;

    public int getExceptionRate() {
        return exceptionRate;
    }

    public LogProperties setExceptionRate(int exceptionRate) {
        this.exceptionRate = exceptionRate;
        return this;
    }
}
