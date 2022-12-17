package io.seata.spring;

import io.seata.commonapi.interceptor.parser.TargetClassParser;
import io.seata.spring.util.SpringProxyUtils;

/**
 * @author leezongjie
 * @date 2022/12/17
 */
public class SpringTargetClassParser implements TargetClassParser {
    @Override
    public Class<?> findTargetClass(Object target) throws Exception {
        return SpringProxyUtils.findTargetClass(target);
    }

    @Override
    public Class<?>[] findInterfaces(Object target) throws Exception {
        return SpringProxyUtils.findInterfaces(target);
    }
}
