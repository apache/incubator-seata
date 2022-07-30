package io.seata.rm.datasource.exec.handler;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lyx
 */
public class AfterHandlerFactory {

    private static final Map<String, AfterHandler> AFTER_HANDLER_MAP = new ConcurrentHashMap<>();

    /**
     * get after handler
     *
     * @param sqlType the SQL type
     * @return AfterHandler
     */
    public static AfterHandler getAfterHandler(String sqlType) {
        return CollectionUtils.computeIfAbsent(AFTER_HANDLER_MAP, sqlType,
                key -> EnhancedServiceLoader.load(AfterHandler.class, sqlType));
    }
}
