package io.seata.config.processor;

import io.seata.common.loader.EnhancedServiceLoader;

import java.io.IOException;
import java.util.Properties;

/**
 * The Config Processor.
 *
 * @author zhixing
 */
public class ConfigProcessor {

    public static Properties loadConfig(String config,String dataType) throws IOException {
        return EnhancedServiceLoader.load(Processor.class, dataType).processor(config);
    }

}
