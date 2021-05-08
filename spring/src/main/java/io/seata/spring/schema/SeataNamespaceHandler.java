package io.seata.spring.schema;


import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * The type seata namespace handler
 *
 * @author xingfudeshi@gmail.com
 */
public class SeataNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        super.registerBeanDefinitionParser("application", new SeataBeanDefinitionParser());
    }
}
