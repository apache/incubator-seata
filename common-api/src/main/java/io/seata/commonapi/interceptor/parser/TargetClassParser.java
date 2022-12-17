package io.seata.commonapi.interceptor.parser;

/**
 * @author leezongjie
 * @date 2022/12/17
 */
public interface TargetClassParser {

    Class<?> findTargetClass(Object target) throws Exception;

    Class<?>[] findInterfaces(Object target) throws Exception;

}
