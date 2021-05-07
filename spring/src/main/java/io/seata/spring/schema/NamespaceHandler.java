package io.seata.spring.schema;


import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author xingfudeshi@gmail.com
 * @date 2021/04/30
 */
public class NamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        super.registerBeanDefinitionParser("application", new ApplicationBeanDefinitionParser(ApplicationBean.class));
    }
}
