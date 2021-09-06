package io.seata.config.processor;

import java.io.IOException;
import java.util.Properties;

/**
 * The processing configuration.
 *
 * @author zhixing
 */
public interface Processor {

    /**
     * processing configuration
     *
     * @param config
     * @return the properties
     */
    Properties processor(String config) throws IOException;
}
