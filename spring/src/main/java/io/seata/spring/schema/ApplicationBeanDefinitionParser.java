package io.seata.spring.schema;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author xingfudeshi@gmail.com
 * @date 2021/04/30
 */
public class ApplicationBeanDefinitionParser implements BeanDefinitionParser {
    private final Class<?> beanClass;

    public ApplicationBeanDefinitionParser(Class<?> beanClass) {
        this.beanClass = beanClass;
    }


    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return doParse(element, parserContext, beanClass);
    }

    /**
     * do parse
     *
     * @param element
     * @param parserContext
     * @param beanClass
     * @return org.springframework.beans.factory.config.BeanDefinition
     * @author xingfudeshi@gmail.com
     */
    private static BeanDefinition doParse(Element element, ParserContext parserContext, Class<?> beanClass) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);

        fillPropertyValue(beanDefinition, element);
        parserContext.getRegistry().registerBeanDefinition(beanClass.getName(), beanDefinition);
        return beanDefinition;
    }

    /**
     * fill property value
     *
     * @param beanDefinition
     * @param element
     * @return void
     * @author xingfudeshi@gmail.com
     */
    private static void fillPropertyValue(RootBeanDefinition beanDefinition, Element element) {
        beanDefinition.getPropertyValues().addPropertyValue("applicationId", element.getAttribute("applicationId"));
        beanDefinition.getPropertyValues().addPropertyValue("txServiceGroup", element.getAttribute("txServiceGroup"));
        beanDefinition.getPropertyValues().addPropertyValue("failureHandler", element.getAttribute("failureHandler"));
        beanDefinition.getPropertyValues().addPropertyValue("mode", element.getAttribute("mode"));
        NodeList nodeList = element.getChildNodes();
        int len = nodeList.getLength();
        List<GtxBean> gtx = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element nodeEle = (Element) node;
                GtxBean gtxBean = new GtxBean();
                gtxBean.setName(nodeEle.getAttribute("name"));
                //TODO property fill and null(or blank) checking

                gtx.add(gtxBean);
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue("gtx", gtx);
    }
}
