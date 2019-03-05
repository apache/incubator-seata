package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.rm.tcc.remoting.RemotingParser;

/**
 * @author zhangsen
 */
public abstract class AbstractedRemotingParser implements RemotingParser {


    @Override
    public boolean isRemoting(Object bean, String beanName) throws FrameworkException {
        return isReference(bean, beanName) || isService(bean, beanName);
    }

}
