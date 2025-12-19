package org.apache.seata.integration.tx.api.interceptor.parser;

import org.apache.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodImpl.class);

    @GlobalTransactional(timeoutMills = 300000)
    String dftMethod() {
        LOGGER.info("default method");
        return "default method";
    }

    @GlobalTransactional(timeoutMills = 300000)
    protected String protectedMethod() {
        LOGGER.info("protected method");
        return "protected method";
    }
}
