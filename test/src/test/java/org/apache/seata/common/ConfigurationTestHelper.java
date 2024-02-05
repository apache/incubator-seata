package org.apache.seata.common;

import org.apache.commons.lang.ObjectUtils;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the type ConfigurationTestHelper
 **/
public class ConfigurationTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationTestHelper.class);


    private static final long PUT_CONFIG_TIMEOUT = 30000L;
    private static final long PUT_CONFIG_CHECK_GAP = 500L;

    public static void removeConfig(String dataId) {
        putConfig(dataId, null);
    }

    public static void putConfig(String dataId, String content) {
        ConfigurationCache.addConfigListener(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        if (content == null) {
            System.clearProperty(dataId);
            ConfigurationFactory.getInstance().removeConfig(dataId);
            return;
        }

        System.setProperty(dataId, content);
        ConfigurationFactory.getInstance().putConfig(dataId, content);

        long start = System.currentTimeMillis();
        while (!ObjectUtils.equals(content, ConfigurationFactory.getInstance().getConfig(dataId))) {
            if (PUT_CONFIG_TIMEOUT < System.currentTimeMillis() - start) {
                LOGGER.error("putConfig timeout, dataId={}, timeout={}ms", dataId, PUT_CONFIG_TIMEOUT);
                return;
            }
            try {
                Thread.sleep(PUT_CONFIG_CHECK_GAP);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        LOGGER.info("putConfig ok, dataId={}, cost {}ms", dataId, System.currentTimeMillis() - start);
    }
}
