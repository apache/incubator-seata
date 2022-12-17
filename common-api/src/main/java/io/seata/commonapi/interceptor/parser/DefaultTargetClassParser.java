package io.seata.commonapi.interceptor.parser;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leezongjie
 * @date 2022/12/17
 */
public class DefaultTargetClassParser implements TargetClassParser {

    protected static List<TargetClassParser> allTargetClassParsers = new ArrayList<>();


    private static class SingletonHolder {
        private static final DefaultTargetClassParser INSTANCE = new DefaultTargetClassParser();
    }

    public static DefaultTargetClassParser get() {
        return DefaultTargetClassParser.SingletonHolder.INSTANCE;
    }

    protected DefaultTargetClassParser() {
        initTargetClassParser();
    }

    /**
     * init parsers
     */
    protected void initTargetClassParser() {
        List<TargetClassParser> targetClassParsers = EnhancedServiceLoader.loadAll(TargetClassParser.class);
        if (CollectionUtils.isNotEmpty(targetClassParsers)) {
            allTargetClassParsers.addAll(targetClassParsers);
        }
    }

    @Override
    public Class<?> findTargetClass(Object target) throws Exception {
        for (TargetClassParser targetClassParser : allTargetClassParsers) {
            Class<?> result = targetClassParser.findTargetClass(target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public Class<?>[] findInterfaces(Object target) throws Exception {
        for (TargetClassParser targetClassParser : allTargetClassParsers) {
            Class<?>[] result = targetClassParser.findInterfaces(target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
